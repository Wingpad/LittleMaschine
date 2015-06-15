#ifndef MASCHINE_H
#define MASCHINE_H

#include <fstream>
#include <iostream>
#include <iomanip>
#include <algorithm>
#include <cstdint>

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32) && !defined(__CYGWIN__)
#include <Winsock2.h>
#else
#include <arpa/inet.h>
#endif

#define MEM_SZ        65536
#define NUM_REGS      32
#define SP_INDEX      0x02
#define FLAGS_INDEX   0x1F
#define ZERO_FLAG     0x4
#define SIGN_FLAG     0x8
#define OVERFLOW_FLAG 0x10

#define ADDRESS_OP    0x01
#define IMMEDIATE_OP  0x02

#define DEBUG_MSG     1

#define REG_A0          0x05
#define REG_A1          0x06
#define REG_A2          0x07

#define REG_V0          0x03

#define SYS_INTERRUPT   0x0000
#define INTERRUPT_TABLE 0xFF7F

#define pow2(a) (0x1LL << (a))
#define size_mask(a) (pow2(a) - 1)

using namespace std;

enum isize_t { BYTE_SZ, WORD_SZ, DWORD_SZ };
enum syscall_t {
  STRING_LENGTH,
  STRING_COMPARE,
  READ_CHAR,
  READ_LINE,
  WRITE_CHAR,
  WRITE_STRING,
  WRITE_LINE,
};

enum opcode_t {
  HLT,
  PUSH,
  POP,
  ADD,
  SUB,
  MUL,
  DIV,
  MOD,
  SHL,
  SHR,
  MOV,
  XCHG,
  AND,
  OR,
  XOR,
  NAND,
  NOR,
  XNOR,
  NOT,
  CMP,
  J,
  JE,
  JNE,
  JG,
  JGE,
  JL,
  JLE,
  CALL,
  RET,
  INTERRUPT,
  LEA,
};

class LittleMaschine {
private:
  bool      halted;
  bool      debugMsg;
  uint32_t  pc;
  uint8_t   mainMemory[MEM_SZ];
  uint32_t  registers[NUM_REGS];

  uint8_t   get_actual_size(isize_t sz);
  uint8_t*  get_ptr(uint8_t regAddress, uint8_t mode, bool ptr, uint8_t size = 32);

  uint32_t  resolve_ptr(uint8_t* ptr, uint8_t size, bool signOp);

  uint32_t  sign_extend(uint32_t val, uint8_t sz);
  void      set_ptr(uint8_t* dst, uint8_t size, uint32_t val);

  bool      has_src(opcode_t opcode);
  bool      has_dst(opcode_t opcode);
  bool      writes_back(opcode_t opcode);
  void      push_pc_and_jump(uint32_t newPc);

  void      handle_interrupt(uint32_t entry);
public:
  LittleMaschine(bool debugMsg = false);

  void start_vm();

  void set_mem_val(uint32_t address, uint8_t val);
  void set_register_val(uint8_t address, uint32_t val);

  uint8_t* get_memory();

  void mem_dump(uint32_t nBytes, uint32_t offset = 0);
  void print_registers();
};

#endif
