#ifndef MASCHINE_H
#define MASCHINE_H

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

#define DEBUG_MSG     1

#define pow2(a) (0x1LL << (a))
#define size_mask(a) (pow2(a) - 1)

using namespace std;

enum amode_t { REG_MODE, IMM_MODE, ABS_MODE, PTR_MODE };
enum isize_t { BYTE_SZ, WORD_SZ, DWORD_SZ };
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
  uint8_t*  get_ptr(uint8_t regAddress, amode_t mode, bool ptr, uint8_t size = 32);

  uint32_t  sign_extend(uint32_t val, uint8_t sz);
  void      set_ptr(uint8_t* dst, uint8_t size, uint32_t val);

  bool      has_src(opcode_t opcode);
  bool      has_dst(opcode_t opcode);
public:
  LittleMaschine(bool debugMsg = false);

  void start_vm();

  void set_mem_val(uint32_t address, uint8_t val);
  void set_register_val(uint8_t address, uint32_t val);

  void mem_dump(uint32_t nBytes = MEM_SZ);
  void print_registers();
};

#endif
