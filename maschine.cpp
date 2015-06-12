/**
 * Compile with "g++ maschine.cpp -std=c++11 -lws2_32" on windows
 */

#include "maschine.hpp"

int main (int argc, const char* argv[]) {
  if (argc < 2) {
    cerr << "Usage is " << argv[0] << " <filename>" << endl;
    return -1;
  }

  cout << "******Start*****" << endl;

  LittleMaschine *maschine = new LittleMaschine(true);
  ifstream infile(argv[1], ios::binary | ios::in);

  infile.seekg (0, infile.end);
  int length = infile.tellg();
  infile.seekg (0, infile.beg);

  infile.read((char*) maschine->get_memory(), length);

  infile.close();

  maschine->mem_dump(16);

  maschine->start_vm();

  delete maschine;

  cout << "*****Finish*****" << endl;

  return 0;
}

LittleMaschine::LittleMaschine(bool debugMsg) {
  this->halted = false;
  this->debugMsg = debugMsg;

  fill(registers, registers+NUM_REGS, 0);
  fill(mainMemory, mainMemory+MEM_SZ, 0);
}

void LittleMaschine::set_mem_val(uint32_t address, uint8_t val) {
  mainMemory[address] = val;
}

void LittleMaschine::set_register_val(uint8_t address, uint32_t val) {
  if (address != 0) {
    registers[address] = ntohl(val);
  }
}

void LittleMaschine::start_vm() {
  while(!halted) {
    // We are first and foremost a little endian architecture, htonl provides
    //  a pretty way to convert to little endianess when needed
    uint32_t  flags   = ntohl(registers[FLAGS_INDEX]);
    uint32_t  instr   = htonl(*((uint32_t*) &mainMemory[pc])) >> 8;
    opcode_t  opcode  = (opcode_t)((instr >> 19) & 0x1F);
    uint8_t   a1m     = (instr >> 17) & 0x03;
    uint8_t   a2m     = (instr >> 15) & 0x03;
    uint8_t   srcReg  = (instr >> 10) & 0x1F;
    uint8_t   dstReg  = (instr >> 5)  & 0x1F;
    uint8_t   size    = get_actual_size((isize_t)((instr >> 3)  & 0x03));
    uint8_t   signOp  = instr & 0x04;
    bool      ptr1    = instr & 0x02;
    bool      ptr2    = instr & 0x01;
    // Now that we're done "decoding" the instruction, increment pc & parse it
    pc += 3;
    uint32_t  srcVal  = 0;
    uint32_t  dstVal  = 0;
    uint8_t   *dst    = NULL;
    uint8_t   *src    = NULL;

    if (has_src(opcode)) {
      src     = get_ptr(srcReg, a1m, ptr1, size);
      srcVal  = htonl(*((uint32_t*)src)) & size_mask(size);

      if (signOp) {
        srcVal = sign_extend(srcVal, size);
      }
    }

    if (has_dst(opcode)) {
      if (a2m & IMMEDIATE_OP) {
        cerr << "Instruction cannot have an immediate destination.";
        break;
      } else if (dstReg == 0) {
        dst     = NULL;
        dstVal  = 0;
      } else {
        dst     = get_ptr(dstReg, a2m, ptr2);
        dstVal  = htonl(*((uint32_t*)dst)) & size_mask(size);
      }

      if (signOp) {
        dstVal = sign_extend(dstVal, size);
      }
    }

    if (debugMsg) {
      cout << endl << "Machine State:" << endl;
      cout << "PC: " << hex << pc << endl;
      cout << "Instr: " << instr << endl;
      cout << "Opcode: " << (int) opcode << endl;
      cout << "A1M/A2M: " << (int) a1m << "/" << (int) a2m << endl;
      cout << "Src/Dst Reg: " << (int) srcReg << "/" << (int) dstReg << endl;
      cout << "SrcVal/DstVal: " << srcVal << "/" << dstVal << endl;
      cout << "Size: " << dec << (int) size << endl;

      if (signOp) {
        cout << "This is a signed operation." << endl;
      }

      print_registers();
    }

    switch(opcode) {
      case HLT:
        halted = true;
        break;
      case PUSH: {
        uint32_t sp = ntohl(registers[SP_INDEX]);
        set_ptr(&mainMemory[sp], size, ntohl(srcVal));
        registers[SP_INDEX] = htonl(sp + (size / 8));
        break;
      }
      case POP: {
        uint32_t sp = ntohl(registers[SP_INDEX]);
        sp -= (size / 8);
        set_ptr(dst, size, *((uint32_t*) (&mainMemory[sp])));
        registers[SP_INDEX] = htonl(sp);
        break;
      }
      case ADD:
        set_ptr(dst, size, srcVal + dstVal);
        break;
      case SUB:
        set_ptr(dst, size, srcVal - dstVal);
        break;
      case MUL:
        set_ptr(dst, size, srcVal * dstVal);
        break;
      case DIV:
        set_ptr(dst, size, srcVal / dstVal);
        break;
      case MOD:
        set_ptr(dst, size, srcVal % dstVal);
        break;
      case SHL:
        set_ptr(dst, size, srcVal << dstVal);
        break;
      case SHR:
        set_ptr(dst, size, srcVal >> dstVal);
        break;
      case MOV:
        set_ptr(dst, size, srcVal);
        break;
      case XCHG:
        set_ptr(dst, size, srcVal);
        set_ptr(src, size, dstVal);
        break;
      case AND:
        set_ptr(dst, size, (srcVal & dstVal));
        break;
      case OR:
        set_ptr(dst, size, (srcVal | dstVal));
        break;
      case XOR:
        set_ptr(dst, size, (srcVal ^ dstVal));
        break;
      case NAND:
        set_ptr(dst, size, ~(srcVal & dstVal));
        break;
      case NOR:
        set_ptr(dst, size, ~(srcVal | dstVal));
        break;
      case XNOR:
        set_ptr(dst, size, ~(srcVal ^ dstVal));
        break;
      case NOT:
        set_ptr(dst, size, ~dstVal);
        break;
      case J:
        pc = dstVal;
        break;
      case JE:
        if (flags & 0x1) {
          pc = dstVal;
        }
        break;
      case JNE:
        if (!(flags & 0x1)) {
          pc = dstVal;
        }
        break;
      case JG:
        if (flags & 0x2) {
          pc = dstVal;
        }
        break;
      case JGE:
        if (flags & 0x3) {
          pc = dstVal;
        }
        break;
      case JL:
        if (!(flags & 0x3)) {
          pc = dstVal;
        }
        break;
      case JLE:
        if (!(flags & 0x2)) {
          pc = dstVal;
        }
        break;
      case CALL: {
        // Grab the SP
        uint32_t sp = ntohl(registers[SP_INDEX]);
        // Push it the PC onto the Stack
        set_ptr(&mainMemory[sp], 4, ntohl(pc));
        // Adjust the SP
        registers[SP_INDEX] = htonl(sp + 4);
        // Then move the PC to the destination
        pc = dstVal;
        break;
      }
      case RET: {
        uint32_t sp = ntohl(registers[SP_INDEX]) - 4;
        pc = *((uint32_t*) (&mainMemory[sp]));
        registers[SP_INDEX] = htonl(sp);
        break;
      }
      case CMP:
        registers[FLAGS_INDEX] = ((srcVal == dstVal) | (srcVal > dstVal) << 1);
        break;
      case LEA:
      case INTERRUPT:
      default:
        cerr << "Unsupported Instruction." << endl;
        break;
    }
  }
}

uint8_t* LittleMaschine::get_memory() {
  return mainMemory;
}

uint32_t LittleMaschine::sign_extend(uint32_t val, uint8_t size) {
  // If the sign bit is high
  if (val & pow2(size - 1)) {
    // Set the upper bits of the number to 1's
    val |= ((size_mask(32 - size)) << size);
  }
  return val;
}

void LittleMaschine::print_registers() {
  cout << "Registers:" << endl;
  for (int i = 0; i < NUM_REGS; i++) {
    cout << "R" << dec << i << ": " << hex << ntohl(registers[i]) << "\t";

    if ((i % (NUM_REGS / 8)) == 0 && i != 0) {
      cout << endl;
    }
  }
  cout << endl;
}

void LittleMaschine::mem_dump(uint32_t nBytes) {
  cout << "Memory:" << endl;
  for (int i = 0; i < nBytes; i++) {
    cout << setfill('0') << setw(2) << hex << (int) mainMemory[i] << " ";

    if (((i + 1) % 8) == 0 && i != 0) {
      cout << endl;
    }
  }
}

bool LittleMaschine::has_src(opcode_t opcode) {
  bool result;

  switch(opcode) {
    case HLT:
    case POP:
    case NOT:
    case J:
    case JE:
    case JNE:
    case JG:
    case JGE:
    case JL:
    case JLE:
    case RET:
    case CALL:
    case INTERRUPT:
      result = false;
      break;
    default:
      result = true;
      break;
  }

  return result;
}

bool LittleMaschine::has_dst(opcode_t opcode) {
  bool result;

  switch(opcode) {
    case HLT:
    case PUSH:
    case RET:
      result = false;
      break;
    default:
      result = true;
      break;
  }

  return result;
}

uint8_t LittleMaschine::get_actual_size(isize_t sz) {
  uint8_t size;

  switch (sz) {
    case BYTE_SZ:
      size = 8;
      break;
    case WORD_SZ:
      size = 16;
      break;
    case DWORD_SZ:
      size = 32;
      break;
  }

  return size;
}

uint8_t* LittleMaschine::get_ptr(uint8_t regAddress, uint8_t mode, bool ptr, uint8_t size) {
  uint32_t value;
  uint8_t* address = NULL;

  // If this is an address operation
  if (mode & ADDRESS_OP) {
    // Fix the size at 32-bits
    size = 32;
  }

  // If this is an immediate operation
  if (mode & IMMEDIATE_OP) {
    // Grab the immediate value's address
    address = &mainMemory[pc];
    // And Increment the PC based on what we're reading
    pc += (size / 8);
  } else {
    // Otherwise, if it is a register address, grab the address of the register
    address = (uint8_t*) (&registers[regAddress]);
  }

  // If it is an address operation
  if (mode & ADDRESS_OP) {
    // Resolve the pointer in host ordering
    value = ntohl(*((uint32_t*) address));
    // and use it as a pointer to main memory
    address = (uint8_t*) (&mainMemory[value & 0xFFFF]);
  }

  // If it is a pointer
  if (ptr) {
    // You have to repeat this operation again
    value = ntohl(*((uint32_t*) address));
    address = (uint8_t*) (&mainMemory[value & 0xFFFF]);
  }

  return address;
}

void LittleMaschine::set_ptr(uint8_t* dst, uint8_t size, uint32_t val) {
  if (dst != NULL) {
    val &= size_mask(size);
    val |= ntohl(*((uint32_t*)dst)) & ((size_mask(32 - size)) << size);
    *((uint32_t*)dst) = htonl(val);
  }
}
