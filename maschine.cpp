/**
 * Compile with "g++ maschine.cpp -std=c++11 -lws2_32" on windows
 */

#include "maschine.hpp"

int main (int argc, const char* argv[]) {
  cout << "****Start****" << endl;

  // 0x09 8C 10
  mainMemory[0] = 0x09;
  mainMemory[1] = 0x8C;
  mainMemory[2] = 0x10;

  // 0x18 08 70
  mainMemory[3] = 0x18;
  mainMemory[4] = 0x08;
  mainMemory[5] = 0x70;

  // 0x16 00 70
  mainMemory[6] = 0x16;
  mainMemory[7] = 0x00;
  mainMemory[8] = 0x70;

  registers[2] = ntohl(0x00001000);
  registers[3] = ntohl(0x10001001);

  start_vm();

  return 0;
}

void start_vm() {
  bool halted = false;

  do {
    uint32_t pc       = ntohl(registers[PC_INDEX]);
    // We are first and foremost a little endian architecture, htonl provides
    //  a pretty way to convert to little endianess when needed
    uint32_t  instr   = htonl(*((uint32_t*) &mainMemory[pc])) >> 8;
    opcode_t  opcode  = (opcode_t)((instr >> 19) & 0x1F);
    amode_t   a1m     = (amode_t)((instr >> 17) & 0x03);
    amode_t   a2m     = (amode_t)((instr >> 15) & 0x03);
    uint8_t   srcReg  = (instr >> 10) & 0x1F;
    uint8_t   dstReg  = (instr >> 5)  & 0x1F;
    uint8_t   size    = get_actual_size((isize_t)((instr >> 3)  & 0x03));
    uint8_t   signOp  = instr & 0x04;
    bool      ptr1    = instr & 0x02;
    bool      ptr2    = instr & 0x01;
    // Now that we're done "decoding" the instruction, increment pc & parse it
    pc += 3;
    uint32_t  srcVal, dstVal;
    uint8_t *dst;

    if (has_src(opcode)) {
      srcVal  = get_value(pc, srcReg, size, a1m, ptr1);
    }

    if (has_dst(opcode)) {
      if (a2m == IMM_MODE) {
        cerr << "Instruction cannot have an immediate destination.";
        break;
      }
      dst     = get_dst(pc, dstReg, a2m, ptr2);
      dstVal  = htonl(*((uint32_t*)dst)) & size_mask(size);
    }

#ifdef DEBUG_MSG
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
#endif

    switch(opcode) {
      case HLT:
        halted = true;
        break;
      case PUSH: {
        uint32_t sp = ntohl(registers[SP_INDEX]);
        set_dst(&mainMemory[sp], size, srcVal);
        registers[SP_INDEX] = htonl(sp + (size / 8));
        break;
      }
      case POP: {
        uint32_t sp = ntohl(registers[SP_INDEX]);
        sp -= (size / 8);
        set_dst(dst, size, *((uint32_t*) (&mainMemory[sp])));
        registers[SP_INDEX] = htonl(sp);
        break;
      }
      case ADD:
        if (signOp) {
          cout << "WARNING: Signed arithmetic not performed..." << endl;
        } else {
          set_dst(dst, size, srcVal + dstVal);
        }
        break;
    }

    registers[PC_INDEX] = htonl(pc);
  } while(!halted);
}

void print_registers() {
  cout << "Registers:" << endl;
  for (int i = 0; i < NUM_REGS; i++) {
    cout << "R" << dec << i << ": " << hex << ntohl(registers[i]) << "\t";

    if ((i % (NUM_REGS / 8)) == 0 && i != 0) {
      cout << endl;
    }
  }
  cout << endl;
}

bool has_src(opcode_t opcode) {
  bool result;

  switch(opcode) {
    case POP:
    case HLT:
    case J:
    case JZ:
    case JNZ:
    case JG:
    case JGE:
    case JL:
    case JLE:
    case INTERRUPT:
      result = false;
      break;
    default:
      result = true;
      break;
  }

  return result;
}

bool has_dst(opcode_t opcode) {
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

uint8_t get_actual_size(isize_t sz) {
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

uint32_t get_value(uint32_t &pc, uint8_t regAddress, uint8_t size, amode_t mode, bool ptr) {
  uint32_t value;

  switch(mode) {
    case REG_MODE:
      // V = R[Rg]
      value = registers[regAddress];
      break;
    case IMM_MODE:
      // V = I
      value = *((uint32_t*) &mainMemory[pc]);
      // Increment PC based on what we're reading
      pc += (size / 8);
      break;
    case ABS_MODE:
      // TODO Check if ntohl is needed!
      // V = A
      value = ntohl(*((uint32_t*) &mainMemory[pc]));
      // V = M[A]
      value &= 0xFFFF;
      value = *((uint32_t*) &mainMemory[value]);
      // Increment PC to account for Address
      pc += 4;
      break;
  }

  if (ptr) {
    // V = *M[A]
    value &= 0xFFFF;
    value = *((uint32_t*) &mainMemory[value]);
  }

  return htonl(value) & size_mask(size);
}

uint8_t* get_dst(uint32_t& pc, uint8_t regAddress, amode_t mode, bool ptr) {
  uint32_t value;
  uint8_t* address = NULL;

  switch(mode) {
    case REG_MODE:
      // Val := &R[RegNum]
      address = (uint8_t*) (&registers[regAddress]);
      break;
    case ABS_MODE:
      // V := *PC
      value = ntohl(*((uint32_t*) &mainMemory[pc]));
      // Address := &M[Val]
      address = (uint8_t*) (&mainMemory[value & 0xFFFF]);
      // Increment PC to account for Reading the Address
      pc += 4;
      break;
  }

  if (ptr) {
    // Val := *Address
    value = ntohl(*((uint32_t*) address));
    // Address := &M[Val]
    address = (uint8_t*) (&mainMemory[value & 0xFFFF]);
  }

  return address;
}

void set_dst(uint8_t* dst, uint8_t size, uint32_t val) {
  val &= size_mask(size);
  val |= ntohl(*((uint32_t*)dst)) & ((size_mask(32 - size)) << size);
  *((uint32_t*)dst) = htonl(val);
}
