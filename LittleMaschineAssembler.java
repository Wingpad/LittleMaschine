import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
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

  public Object addOperand(LittleMaschineParser.OperandContext ctx) {
    return addOperand(ctx, mCurrentStatement);
  }

  public Object addOperand(LittleMaschineParser.OperandContext ctx, LittleMaschineStatement statement) {
    LittleMaschineOperand operand;
    // If the operand is not an Address Expression
    if (ctx.addressExpression() == null) {
      // Simply create a basic operand based on the context
      operand = createBasicOperand(ctx);
      // and add it to the statement
      statement.addOperand(operand);
    }
    // Otherwise, visit the Address Expression
    return visitChildren(ctx);
  }

  public LittleMaschineOperand createBasicOperand(LittleMaschineParser.OperandContext ctx) {
    LittleMaschineOperand operand = new LittleMaschineOperand();
    // Handle each "simple" type of operand, accordingly
    if (ctx.Register() != null) {
      operand.setValue(LittleMaschineRegister.getEnum(ctx.Register().toString()));
    } else if (ctx.Literal() != null) {
      operand.setValue(LittleMaschineUtils.parseLiteral(ctx.Literal()));
    } else if (ctx.Identifier() != null) {
      operand.setValue(LittleMaschineIdentifier.get(ctx.Identifier().toString()));
    }
    return operand;
  }

  public void adjustSize(ParserRuleContext ctx) {
    Object sizeSpecifier;
    try {
      Method method = ctx.getClass().getDeclaredMethod("SizeSpecifier");
      sizeSpecifier = method.invoke(ctx);
    } catch (Exception e) {
      sizeSpecifier = null;
    }

    if (sizeSpecifier != null) {
      char size = sizeSpecifier.toString().charAt(0);

      switch (size) {
        case 'b':
          mCurrentStatement.setSize(1);
          break;
        case 'w':
          mCurrentStatement.setSize(2);
          break;
        case 'd':
          mCurrentStatement.setSize(4);
          break;
      }
    }
  }

  public void printProgram() {
    int address = 0;
    for (Object o : mProgram) {
      System.out.println(String.format("%04X : %s", address, o));

      if (o instanceof LittleMaschineStatement) {
        address += ((LittleMaschineStatement) o).calcSize();
      }
    }
  }

  @Override
  public Object visitLabel(LittleMaschineParser.LabelContext ctx) {
    // Simply add the label to the program
    mProgram.add(LittleMaschineIdentifier.get(ctx.Identifier().toString()));
    return null;
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
    // Adjust the size
    adjustSize(ctx);
    // Then return null
    return null;
  }

  @Override
  public Object visitSingleOperandInstr(LittleMaschineParser.SingleOperandInstrContext ctx) {
    // Adjust the size
    adjustSize(ctx);
    // Visit the kids
    visitChildren(ctx);
    // Set the opcode
    mCurrentStatement.setOpcode(ctx.SingleOperandOpcode().toString());
    // Then return null
    return null;
  }

  @Override
  public Object visitDualOperandInstr(LittleMaschineParser.DualOperandInstrContext ctx) {
    // Adjust the size
    adjustSize(ctx);
    // Visit the kids
    visitChildren(ctx);
    // Set the opcode
    mCurrentStatement.setOpcode(ctx.DualOperandOpcode().toString());
    // Then return null
    return null;
  }

  @Override
  public Object visitOperand(LittleMaschineParser.OperandContext ctx) {
    return addOperand(ctx);
  }

  @Override
  public Object visitAddressExpression(LittleMaschineParser.AddressExpressionContext ctx) {
    // Grab the context's expression
    LittleMaschineParser.ExpressionContext expression = ctx.expression();
    LittleMaschineOperand operand;
    // If there is only one child
    if (expression.getChildCount() == 1) {
      // Simply use it's value as the context
      operand = createBasicOperand(expression.operand(0));
      // Setting its addressing accordingly
      operand.setAddressingMode(true, ctx.Pointer() != null);
      // And adding it to the statement
      mCurrentStatement.addOperand(operand);
    } else {
      // Otherwise more complex logic is needed...
      System.out.println("WARNING: Expressions are not yet supported.");
    }
    // We don't need to visit any of the kids
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

  public static class LittleMaschineStatement {
    private LittleMaschineOpcode mOpcode;
    private ArrayList<LittleMaschineOperand> mOperands;
    private int size = 4;

    public LittleMaschineStatement() {
      mOperands = new ArrayList<LittleMaschineOperand>();
    }

    public void setOpcode(String opcode) {
      mOpcode = LittleMaschineOpcode.valueOf(opcode.toUpperCase());

      if (mOpcode.isVirtualOpcode()) {
        LittleMaschineOperand operand = null;

        switch(mOpcode) {
          case INC:
            mOpcode = LittleMaschineOpcode.ADD;
            operand = new LittleMaschineOperand();
            operand.setValue(1);
            break;
          case DEC:
            mOpcode = LittleMaschineOpcode.SUB;
            operand = new LittleMaschineOperand();
            operand.setValue(1);
            break;
          case NOP:
            operand = new LittleMaschineOperand();
            operand.setValue(LittleMaschineRegister.ZERO);
            addOperand(operand);
            // NOTE this is intentionally falling through!
          case CLR:
            mOpcode = LittleMaschineOpcode.XOR;
            operand = mOperands.get(0);
            break;
        }

        addOperand(operand);
      }
    }

    public boolean addOperand(LittleMaschineOperand operand) {
      if (operand == null) return false;
      else return mOperands.add(operand);
    }

    public void setSize(int size) {
      this.size = size;
    }

    public int calcSize() {
      int calculatedSize = 3;

      for (LittleMaschineOperand operand : mOperands) {
        calculatedSize += operand.calcSize(size);
      }

      return calculatedSize;
    }

    @Override
    public String toString() {
      return mOpcode + " " + mOperands;
    }
  }

  public static class LittleMaschineOperand {
    private boolean mAddress;
    private boolean mPointer;

    private Object mValue;
    private ArrayList<LittleMaschineStatement> mStatements;

    public LittleMaschineOperand() {
      mStatements = new ArrayList<LittleMaschineStatement>();
    }

    public int calcSize(int immSize) {
      int calculatedSize;
      // If the operand is a register
      if (mValue instanceof LittleMaschineRegister) {
        // We start with no penalty
        calculatedSize = 0;
      } else if (mAddress) {
        // If it's address we incur 4 bytes
        calculatedSize = 4;
      } else {
        // otherwise, it's an immediate so we incur X bytes
        calculatedSize = immSize;
      }
      // Then for each of the statements tagging along
      for (LittleMaschineStatement statement : mStatements) {
        // Add their calculatedSize
        calculatedSize += statement.calcSize();
      }
      return calculatedSize;
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

  public static enum LittleMaschineRegister {

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
      // If it is a Register string
      if (str.startsWith("$")) {
        // Grab everything except for the '$'
        str = str.substring(1, str.length());
        // And check if it is an Integer
        if ((i = LittleMaschineUtils.tryParseInt(str)) != null) {
          // If it is, find the appropriate enum
          for (LittleMaschineRegister reg : values()) {
            if (i.equals(reg.address)) {
              return reg;
            }
          }
        }
      }
      // Otherwise, hand it off to the default valueOf function
      return valueOf(str.toUpperCase());
    }
  }

  public static enum LittleMaschineOpcode {

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

    public boolean isVirtualOpcode() {
      return opcode == VIRTUAL_INSTR;
    }
  }

  public static class LittleMaschineIdentifier {
    public final String name;
    private int mAddress;

    private static HashMap<String, LittleMaschineIdentifier> sIdentifiers;

    static {
      sIdentifiers = new HashMap<String, LittleMaschineIdentifier>();
    }

    private LittleMaschineIdentifier(String name) {
      this.name = name;
    }

    public void setAddress(int address) {
      mAddress = address;
    }

    public int getAddress() {
      return mAddress;
    }

    @Override
    public String toString() {
      return name;
    }

    public static LittleMaschineIdentifier get(String name) {
      if (!sIdentifiers.containsKey(name)) {
        sIdentifiers.put(name, new LittleMaschineIdentifier(name));
      }

      return sIdentifiers.get(name);
    }
  }
}
