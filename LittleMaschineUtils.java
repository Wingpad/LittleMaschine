import org.antlr.v4.runtime.tree.TerminalNode;

public class LittleMaschineUtils {

  public static byte toByte(boolean bool) {
    return (byte) (bool ? 1 : 0);
  }

  public static Object parseLiteral(TerminalNode node) {
    String text = node.getText();

    if (text.startsWith("\"") || text.startsWith("\'")) {
      return unescapeJavaString(text.substring(1, text.length() - 1));
    } else if (text.startsWith("0x")) {
      return Integer.parseInt(text.substring(2, text.length()), 16);
    } else if (text.startsWith("0b")) {
      return Integer.parseInt(text.substring(2, text.length()), 2);
    } else if (text.startsWith("0")) {
      return Integer.parseInt(text, 8);
    } else {
      return Integer.parseInt(text);
    }
  }

  public static Integer tryParseInt(String str)
  {
    Integer i;
    try {
      i = Integer.parseInt(str);
    } catch(NumberFormatException nfe) {
      return null;
    }
    return i;
  }

  public static String unescapeJavaString(String st) {
    StringBuilder sb = new StringBuilder(st.length());

    for (int i = 0; i < st.length(); i++) {
      char ch = st.charAt(i);
      if (ch == '\\') {
        char nextChar = (i == st.length() - 1) ? '\\' : st.charAt(i + 1);
        // Octal escape?
        if (nextChar >= '0' && nextChar <= '7') {
          String code = "" + nextChar;
          i++;
          if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
            code += st.charAt(i + 1);
            i++;
            if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
              code += st.charAt(i + 1);
              i++;
            }
          }
          sb.append((char) Integer.parseInt(code, 8));
          continue;
        }
        switch (nextChar) {
          case '\\':
            ch = '\\';
            break;
          case 'b':
            ch = '\b';
            break;
          case 'f':
            ch = '\f';
            break;
          case 'n':
            ch = '\n';
            break;
          case 'r':
            ch = '\r';
            break;
          case 't':
            ch = '\t';
            break;
          case '\"':
            ch = '\"';
            break;
          case '\'':
            ch = '\'';
            break;
            // Hex Unicode: u????
          case 'u':
            if (i >= st.length() - 5) {
              ch = 'u';
              break;
            }
            int code = Integer.parseInt(
              "" + st.charAt(i + 2) + st.charAt(i + 3) + st.charAt(i + 4) + st.charAt(i + 5), 16);
            sb.append(Character.toChars(code));
            i += 5;
            continue;
        }
        i++;
      }
      sb.append(ch);
    }
    return sb.toString();
  }
}
