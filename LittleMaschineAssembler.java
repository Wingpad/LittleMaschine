import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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

  private void recalculateAddresses() {
    int address = 0;
    for (Object o : mProgram) {
      // If the object is an identifier
      if (o instanceof LittleMaschineIdentifier) {
        // Set its address accordingly
        ((LittleMaschineIdentifier) o).setAddress(address);
        // Then add its size (in case it's a block)
        address += ((LittleMaschineIdentifier) o).getSize();
      } else if (o instanceof LittleMaschineStatement) {
        // Otherwise, if it's just a statement add its calculated size
        address += ((LittleMaschineStatement) o).calcSize();
      }
    }
  }

  public void writeBinaryFile(String outFilepath) throws IOException {
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outFilepath));
    // Start by Recalculating the Addresses for Identifiers
    recalculateAddresses();
    ArrayList<Byte> bytes = new ArrayList<Byte>();
    for (Object o : mProgram) {
      if (o instanceof LittleMaschineStatement) {
        bytes.addAll(((LittleMaschineStatement) o).getBytes());
      }
    }
    byte[] data = new byte[bytes.size()];
    for (int i = 0; i < bytes.size(); i++) {
      data[i] = bytes.get(i).byteValue();
    }
    output.write(data);
    output.close();
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

    LittleMaschineAssembler maschine = new LittleMaschineAssembler(new ANTLRFileStream(inFilepath));
    maschine.printProgram();
    maschine.writeBinaryFile(outFilepath);
  }

  public static class LittleMaschineStatement {
    private LittleMaschineOpcode mOpcode;
    private LinkedList<LittleMaschineOperand> mOperands;
    private int mSize = 4;

    public LittleMaschineStatement() {
      mOperands = new LinkedList<LittleMaschineOperand>();
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
      mSize = size;
    }

    public int calcSize() {
      int calculatedSize = 3;

      for (LittleMaschineOperand operand : mOperands) {
        calculatedSize += operand.calcSize(mSize);
      }

      return calculatedSize;
    }

    public ArrayList<Byte> getBytes() {
      ArrayList<Byte> bytes = new ArrayList<Byte>();
      LittleMaschineOperand op1 = null, op2 = null;
      byte opcode = (byte) mOpcode.opcode;
      byte a1m = 0x3;
      byte a2m = 0x3;
      byte srcReg = 0x1F;
      byte dstReg = 0x1F;
      byte size = (byte) (mSize >> 1);
      // TODO implement Signed/Unsigned
      byte su = 0x1;
      byte pt1 = 0x0, pt2 = 0x0;

      if (mOpcode.hasDst()) {
        op2 = mOperands.poll();
        // Add all of the bytes from the op's statement
        bytes.addAll(op2.getStatementBytes());
        // Adjust the a1m flags
        a2m = (byte)(((byte)(LittleMaschineUtils.toByte(!op2.isRegister()) << 1)) | LittleMaschineUtils.toByte(op2.isAddress()));
        // Set the pointer flag
        pt2 = LittleMaschineUtils.toByte(op2.isPointer());
        // Finally, if it is a register, set the value accordingly
        if (op2.isRegister()) {
          dstReg = (byte) ((LittleMaschineRegister) op2.getValue()).address;
        }
      }

      if (mOpcode.hasSrc()) {
        op1 = mOperands.poll();
        // Add all of the bytes from the op's statement
        bytes.addAll(op1.getStatementBytes());
        // Adjust the a1m flags
        a1m = (byte)(((byte)(LittleMaschineUtils.toByte(!op1.isRegister()) << 1)) | LittleMaschineUtils.toByte(op1.isAddress()));
        // Set the pointer flag
        pt1 = LittleMaschineUtils.toByte(op1.isPointer());
        // Finally, if it is a register, set the value accordingly
        if (op1.isRegister()) {
          srcReg = (byte) ((LittleMaschineRegister) op1.getValue()).address;
        }
      }

      bytes.add((byte)((opcode << 3) | (a1m << 1) | (a2m >> 1)));
      bytes.add((byte)((a2m << 7) | (srcReg << 2) | (dstReg >> 3)));
      bytes.add((byte)((dstReg << 5) | (size << 3) | (su << 2) | (pt1 << 1) | pt2));

      if (op1 != null) {
        bytes.addAll(op1.getOperandBytes(mSize));
      }

      if (op2 != null) {
        bytes.addAll(op2.getOperandBytes(mSize));
      }

      return bytes;
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

    public ArrayList<Byte> getStatementBytes() {
      ArrayList<Byte> bytes = new ArrayList<Byte>();
      for (LittleMaschineStatement statement : mStatements) {
        bytes.addAll(statement.getBytes());
      }
      return bytes;
    }

    public ArrayList<Byte> getOperandBytes(int immSize) {
      ArrayList<Byte> bytes = new ArrayList<Byte>();

      if (isRegister()) {
        return bytes;
      } else if (mValue instanceof LittleMaschineIdentifier) {
        mValue = ((LittleMaschineIdentifier) mValue).getAddress();
      }

      if (isAddress()) {
        immSize = 4;
      }

      if (mValue instanceof String) {
        for (int i = 0; (i < immSize); i++) {
          if (i < ((String) mValue).length()) {
            bytes.add((byte) ((String) mValue).charAt(i));
          } else {
            bytes.add((byte) 0x0);
          }
        }
      } else {
        int value = (int) mValue;
        for (int i = 0; i < 4; i++) {
          byte b = (byte) ((value & 0xFF000000) >> 24);
          if ((3 - i) < immSize) {
            bytes.add(b);
          }
          value <<= 8;
        }
      }

      return bytes;
    }

    public boolean isAddress() {
      return mAddress;
    }

    public boolean isPointer() {
      return mPointer;
    }

    public boolean isRegister() {
      return (mValue instanceof LittleMaschineRegister);
    }

    public Object getValue() {
      return mValue;
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

    public boolean hasSrc() {
      switch(this) {
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
        case INT:
          return false;
        default:
          return true;
      }
    }

    public boolean hasDst() {
      switch(this) {
        case HLT:
        case PUSH:
        case RET:
          return false;
        default:
          return true;
      }
    }
  }

  public static class LittleMaschineIdentifier {
    public final String name;
    private int mAddress;
    private int mSize = 0;

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

    public int getSize() {
      return mSize;
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
