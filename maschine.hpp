#ifndef MASCHINE_H
#define MASCHINE_H

#include <iostream>
#include <cstdint>

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32) && !defined(__CYGWIN__)
#include <Winsock2.h>
#else
#include <arpa/inet.h>
#endif

#define MEM_SZ    65536
#define NUM_REGS  32
#define PC_INDEX  1
#define SP_INDEX  2

#define DEBUG_MSG 1

#define pow2(a) (0x1LL << a)
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
  JZ,
  JNZ,
  JG,
  JGE,
  JL,
  JLE,
  RET,
  INTERRUPT,
};

void start_vm();

uint8_t get_actual_size(isize_t sz);
uint32_t get_value(uint32_t &pc, uint8_t regAddress, uint8_t size, amode_t mode, bool ptr);
uint8_t* get_dst(uint32_t& pc, uint8_t regAddress, amode_t mode, bool ptr);

void set_dst(uint8_t* dst, uint8_t size, uint32_t val);

bool has_src(opcode_t opcode);
bool has_dst(opcode_t opcode);

void print_registers();

uint8_t   mainMemory[MEM_SZ] = {0};
uint32_t  registers[NUM_REGS] = {0};

#endif
