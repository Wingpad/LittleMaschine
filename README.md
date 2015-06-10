#Little Maschine:
Little Machine aims to be a small VM with an x86/MIPS-like architecture. It is a work-in-progress right now and has limited functionality.

##Instructions:
###Stack Operations:
  push      0x01
  pop       0x02

###Arithmetic:
  add       0x03
  sub       0x04
  mul       0x05
  div       0x06
  mod       0x07

  inc       ----  (sub $X, 1)
  dec       ----  (add $X, 1)

  shl       0x08
  shr       0x09

###Register Operations:
  clr       ----  (xor $X, $X)
  mov       0x0A
  xchg      0x0B

###Bitwise Logic:
  and       0x0C
  or        0x0D
  xor       0x0E
  nand      0x0F
  nor       0x10
  xnor      0x11
  not       0x12

###Testing:
  cmp       0x13

###Flow Control:
  j         0x14
  jz/je     0x15
  jnz/jne   0x16
  jg/jnle   0x17
  jge/jnl   0x18
  jl/jnge   0x19
  jle/jng   0x1A

  nop       ----  (xor $0, $0)

  call      ----  (push.dw $pc)
  ret       0x1B

  int       0x1C

  hlt       0x00


##Registers:
  $0      Zero, Always fixed to zero (zero)
  $1      PC, Program Counter (pc)
  $2      SP, Stack Pointer (sp)
  $3-4    Function Result Regs (v0-v1)
  $5-8    Function Argument Regs (a0-a3)
  $8-17   Temporary Regs, not preserved across function calls (t0-t9)
  $18-27  Saved Regs, functions must save/restore their vals (s0-s9)
  $28-29  Reserved for OS kernel use/exception return (k0-k1)
  $30     Flags, see flags fmt for reference (flags)
  $31     Reserved for pseudo-instructions (at)

##Sizes:
  byte    b   8
  word    w   16
  dword   dw  32

##Flags:
  0       CF  Carry Flag
  1       PF  Parity Flag
  2       ZF  Zero Flag
  3       SF  Sign Flag
  4       OF  Overflow Flag
  5-7     NU  Not Used

##Instruction Format:
  7   6   5   4   3   2   1   0   7   6   5   4   3   2   1   0   7   6   5   4   3   2   1   0
  OP-------------->  A1m-->  A2m-->  Src-------------->  Dst-------------->  Sz--->  S/U Pt1 Pt2

  OP      5   OPCode
  A1m/A2m 2   Addressing Modes, 00 Register, 01 Immediate, 10 Absolute
  Src     5   Source Register (Addressing Mode 00 Only)
  Dst     5   Destination Register (Addressing Mode 00 Only)
  Sz      2   Size, 00 byte, 01 word, 10 dword
  S/U     1   If 1 Signed, otherwise Unsigned
  Ptr1    1   If 1 then Arg1 will be interpreted as a ptr
  Ptr2    1   If 1 then Arg2 will be interpreted as a ptr
