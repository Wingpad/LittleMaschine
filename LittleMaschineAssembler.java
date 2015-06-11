import java.io.*;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class LittleMaschineAssembler extends LittleMaschineBaseVisitor<Object> {

  private ArrayList<Object> mProgram;
  private LittleMaschineStatement mCurrentStatement;

  public LittleMaschineAssembler(ANTLRFileStream input) {
    // Create the Lexer to Tokenize the input for the Parser
    LittleMaschineLexer lexer = new LittleMaschineLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    LittleMaschineParser parser = new LittleMaschineParser(tokens);
    // Then grab the parse tree and visit its program
    ParseTree tree = parser.program();
    mProgram = new ArrayList<Object>();
    this.visit(tree);
  }

  public void printProgram() {
    for (Object o : mProgram) {
      System.out.println(o);
    }
  }

  @Override
  public Object visitStatement(LittleMaschineParser.StatementContext ctx) {
    // Initialize the Current Statment
    mCurrentStatement = new LittleMaschineStatement();
    // Visit the kids
    visitChildren(ctx);
    // Then add the current statement to our program
    mProgram.add(mCurrentStatement);
    // Finally returning nothing
    return null;
  }

  @Override
  public Object visitNoOperandInstr(LittleMaschineParser.NoOperandInstrContext ctx) {
    // Set the opcode
    mCurrentStatement.setOpcode(ctx.NoOperandOpcode().toString());
    // then return null
    return null;
  }

  @Override
  public Object visitSingleOperandInstr(LittleMaschineParser.SingleOperandInstrContext ctx) {
    // Set the opcode
    mCurrentStatement.setOpcode(ctx.SingleOperandOpcode().toString());
    return visitChildren(ctx);
  }

  @Override
  public Object visitDualOperandInstr(LittleMaschineParser.DualOperandInstrContext ctx) {
    // Set the opcode
    mCurrentStatement.setOpcode(ctx.DualOperandOpcode().toString());
    return visitChildren(ctx);
  }

  @Override
  public Object visitOperand(LittleMaschineParser.OperandContext ctx) {
    LittleMaschineOperand operand;

    if (ctx.Register() != null) {
      operand = new LittleMaschineOperand();
      operand.setValue(LittleMaschineRegister.getEnum(ctx.Register().toString()));
      mCurrentStatement.addOperand(operand);
    } else if (ctx.Literal() != null) {
      operand = new LittleMaschineOperand();
      operand.setValue(ctx.Literal().toString());
      mCurrentStatement.addOperand(operand);
    } else if (ctx.Identifier() != null) {
      operand = new LittleMaschineOperand();
      operand.setValue(ctx.Identifier().toString());
      mCurrentStatement.addOperand(operand);
    }

    return visitChildren(ctx);
  }

  @Override
  public Object visitAddressExpression(LittleMaschineParser.AddressExpressionContext ctx) {
    return null;
  }

  public void writeBinaryFile(String outFilepath) throws IOException {

  }

  public static void main(String... args) throws IOException {
    String inFilepath, outFilepath;

    if (args.length == 1) {
      inFilepath = args[0];
      outFilepath = "a.lmb";
    } else if (args.length == 2) {
      inFilepath = args[0];
      outFilepath = args[1];
    } else {
      System.err.println("Invalid Arguments, expected lma <file> or lma <file> <output_file>");
      return;
    }

    (new LittleMaschineAssembler(new ANTLRFileStream(inFilepath))).printProgram();
  }

  public class LittleMaschineStatement {
    private LittleMaschineOpcode mOpcode;
    private ArrayList<LittleMaschineOperand> mOperands;

    public LittleMaschineStatement() {
      mOperands = new ArrayList<LittleMaschineOperand>();
    }

    public void setOpcode(String opcode) {
      mOpcode = LittleMaschineOpcode.valueOf(opcode.toUpperCase());
    }

    public boolean addOperand(LittleMaschineOperand operand) {
      return mOperands.add(operand);
    }

    @Override
    public String toString() {
      return mOpcode + " " + mOperands;
    }
  }

  public class LittleMaschineOperand {
    private boolean mAddress;
    private boolean mPointer;

    private Object mValue;
    private ArrayList<LittleMaschineStatement> mStatements;

    public LittleMaschineOperand() {
      mStatements = new ArrayList<LittleMaschineStatement>();
    }

    public void setValue(Object value) {
      mValue = value;
    }

    public void setAddressingMode(boolean absolute, boolean pointer) {
      mAddress = absolute;
      mPointer = pointer;
    }

    @Override
    public String toString() {
      if (mAddress) {
        return (mPointer ? "ptr[" : "[") + mValue + "]";
      } else {
        return mValue.toString();
      }
    }
  }

  public enum LittleMaschineRegister {
    ZERO(0x00), AT(0x01), SP(0x02),
    V0(0x03), V1(0x04),
    A0(0x05), A1(0x06), A2(0x07), A3(0x08),
    T0(0x09), T1(0x0A), T2(0x0B), T3(0x0C), T4(0x0D), T5(0x0E),
      T6(0x0F), T7(0x10), T8(0x11), T9(0x12),
    S0(0x13), S1(0x14), S2(0x15), S3(0x16), S4(0x17), S5(0x18),
      S6(0x19), S7(0x1A), S8(0x1B), S9(0x1C),
    K0(0x1D), K1(0x1E),
    FLAGS(0x1F);

    public final int address;

    private LittleMaschineRegister(int address) {
      this.address = address;
    }

    public static LittleMaschineRegister getEnum(String str) {
      Integer i;

      if (str.startsWith("$")) {
        str = str.substring(1, str.length());

        if ((i = tryParse(str)) != null) {
          for (LittleMaschineRegister reg : values()) {
            if (i.equals(reg.address)) {
              return reg;
            }
          }
        }
      }

      return valueOf(str.toUpperCase());
    }

    public static Integer tryParse(String str)
    {
      Integer i;
      try {
        i = Integer.parseInt(str);
      } catch(NumberFormatException nfe) {
        return null;
      }
      return i;
    }
  }

  public enum LittleMaschineOpcode {

    PUSH(0x01), POP(0x02), ADD(0x03), SUB(0x04), MUL(0x05), DIV(0x06), MOD(0x07),
      SHL(0x08), SHR(0x09), MOV(0x0A), XCHG(0x0B), AND(0x0C), OR(0x0D), XOR(0x0E),
      NAND(0x0F), NOR(0x10), XNOR(0x11), NOT(0x12), CMP(0x13), J(0x14), JE(0x15),
      JNE(0x16), JG(0x17), JGE(0x18), JL(0x19), JLE(0x1A), CALL(0x1B), RET(0x1C),
      INT(0x1D), LEA(0x1E), HLT(0x00),
    INC(0xFF), DEC(0xFF), CLR(0xFF), NOP(0xFF);

    public static final int VIRTUAL_INSTR = 0xFF;
    public final int opcode;

    private LittleMaschineOpcode(int opcode) {
      this.opcode = opcode;
    }
  }
}
