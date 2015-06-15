        mov     $a0, 0x4
        mov.b   $s0, 'A'
loop:   mov.b   $a1, $s0
        inc.b   $s0
        int     0x00
        cmp.b   $s0, 'Z'  ;
        jge     loop      ; 'Z' >= $s0
