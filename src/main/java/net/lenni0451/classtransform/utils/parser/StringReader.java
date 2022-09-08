package net.lenni0451.classtransform.utils.parser;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.util.Locale;

public class StringReader {

    private static final char QUOTE_CHAR = '"';

    private final String string;
    private int cursor;

    public StringReader(final String string) {
        this(string, 0);
    }

    public StringReader(final String string, final int cursor) {
        this.string = string;
        this.cursor = cursor;
    }

    public int getCursor() {
        return this.cursor;
    }

    public void setCursor(final int cursor) {
        this.cursor = cursor;
    }

    public boolean canRead() {
        return this.canRead(1);
    }

    public boolean canRead(final int amount) {
        return this.cursor + amount <= this.string.length();
    }

    public char peek() {
        return this.peek(0);
    }

    public char peek(final int offset) {
        return this.string.charAt(this.cursor + offset);
    }

    public String peekString(final int length) {
        if (this.cursor + length > this.string.length()) throw new IllegalArgumentException("Cannot read " + length + " characters from cursor position");
        return this.string.substring(this.cursor, this.cursor + length);
    }

    public String peekString() {
        int start = this.cursor;
        String s = this.readString();
        this.cursor = start;
        return s;
    }

    public void skip() {
        this.skip(1);
    }

    public void skip(final int amount) {
        this.cursor += amount;
    }

    public void ensureNext(final char c, final boolean allowEnd) {
        if (!this.canRead()) {
            if (!allowEnd) throw new IllegalStateException("Expected '" + c + "' but got end");
            else return;
        }
        if (this.peek() != c) throw new IllegalStateException("Expected '" + c + "' but got '" + this.peek() + "'");
    }

    public char read() {
        return this.string.charAt(this.cursor++);
    }

    public String readAll() {
        String all = this.string.substring(this.cursor);
        this.cursor = this.string.length();
        return all;
    }

    public String readUntil(final char c) {
        StringBuilder builder = new StringBuilder();
        while (this.canRead() && this.peek() != c) builder.append(this.read());
        this.skip();
        return builder.toString();
    }

    public String readString() {
        if (!this.canRead()) return "";

        if (this.peek() == QUOTE_CHAR) return this.readQuotedString();
        else return this.readUnquotedString();
    }

    public String readUnquotedString() {
        return this.readUntil(' ');
    }

    public String readQuotedString() {
        this.ensureNext(QUOTE_CHAR, false);
        this.skip();
        String s = this.readUntil(QUOTE_CHAR);
        this.ensureNext(' ', true);
        this.skip();
        return s;
    }

    public boolean readBoolean() {
        int start = this.cursor;
        String s = this.readString();
        if (s.equalsIgnoreCase("true")) {
            return true;
        } else if (s.equalsIgnoreCase("false")) {
            return false;
        } else {
            this.cursor = start;
            throw new IllegalStateException("Expected boolean but got '" + s + "'");
        }
    }

    public byte readByte() {
        int start = this.cursor;
        String s = this.readString();
        if (s.toLowerCase().endsWith("b")) s = s.substring(0, s.length() - 1);
        try {
            return Byte.parseByte(s);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new IllegalStateException("Expected byte but got '" + s + "'");
        }
    }

    public boolean canReadByte(final boolean requireSuffix) {
        try {
            int start = this.cursor;
            this.readByte();
            this.cursor = start;
            return !requireSuffix || this.peekString().toLowerCase(Locale.ROOT).endsWith("b");
        } catch (Throwable ignored) {
        }
        return false;
    }

    public short readShort() {
        int start = this.cursor;
        String s = this.readString();
        if (s.toLowerCase().endsWith("s")) s = s.substring(0, s.length() - 1);
        try {
            return Short.parseShort(s);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new IllegalStateException("Expected short but got '" + s + "'");
        }
    }

    public boolean canReadShort(final boolean requireSuffix) {
        try {
            int start = this.cursor;
            this.readShort();
            this.cursor = start;
            return !requireSuffix || this.peekString().toLowerCase(Locale.ROOT).endsWith("s");
        } catch (Throwable ignored) {
        }
        return false;
    }

    public int readInt() {
        int start = this.cursor;
        String s = this.readString();
        if (s.toLowerCase().endsWith("i")) s = s.substring(0, s.length() - 1);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new IllegalStateException("Expected integer but got '" + s + "'");
        }
    }

    public boolean canReadInt(final boolean requireSuffix) {
        try {
            int start = this.cursor;
            this.readInt();
            this.cursor = start;
            return !requireSuffix || this.peekString().toLowerCase(Locale.ROOT).endsWith("i");
        } catch (Throwable ignored) {
        }
        return false;
    }

    public long readLong() {
        int start = this.cursor;
        String s = this.readString();
        if (s.toLowerCase().endsWith("l")) s = s.substring(0, s.length() - 1);
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new IllegalStateException("Expected long but got '" + s + "'");
        }
    }

    public boolean canReadLong(final boolean requireSuffix) {
        try {
            int start = this.cursor;
            this.readLong();
            this.cursor = start;
            return !requireSuffix || this.peekString().toLowerCase(Locale.ROOT).endsWith("l");
        } catch (Throwable ignored) {
        }
        return false;
    }

    public float readFloat() {
        int start = this.cursor;
        String s = this.readString();
        if (s.toLowerCase().endsWith("f")) s = s.substring(0, s.length() - 1);
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new IllegalStateException("Expected float but got '" + s + "'");
        }
    }

    public boolean canReadFloat(final boolean requireSuffix) {
        try {
            int start = this.cursor;
            this.readFloat();
            this.cursor = start;
            return !requireSuffix || this.peekString().toLowerCase(Locale.ROOT).endsWith("f");
        } catch (Throwable ignored) {
        }
        return false;
    }

    public double readDouble() {
        int start = this.cursor;
        String s = this.readString();
        if (s.toLowerCase().endsWith("d")) s = s.substring(0, s.length() - 1);
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new IllegalStateException("Expected double but got '" + s + "'");
        }
    }

    public boolean canReadDouble(final boolean requireSuffix) {
        try {
            int start = this.cursor;
            this.readDouble();
            this.cursor = start;
            return !requireSuffix || this.peekString().toLowerCase(Locale.ROOT).endsWith("d");
        } catch (Throwable ignored) {
        }
        return false;
    }

    public int readOpcode() {
        try {
            return this.readInt();
        } catch (Throwable t) {
            return StringParser.OPCODES.get(this.readString().toUpperCase(Locale.ROOT));
        }
    }

    public Type readType() {
        if (!this.peekString(5).equalsIgnoreCase("type(")) throw new IllegalStateException("Expected type but got '" + this.peekString(4) + "'");
        this.skip(5);
        String type = this.readString();
        if (type.charAt(type.length() - 1) != ')') throw new IllegalStateException("Expected ')' but got '" + type.charAt(type.length() - 1) + "'");
        return Type.getType(type.substring(0, type.length() - 1));
    }

    public Handle readHandle() {
        if (!this.peekString(7).equalsIgnoreCase("handle(")) throw new IllegalStateException("Expected handle but got '" + this.peekString(6) + "'");
        this.skip(7);
        int opcode = this.readOpcode();
        String owner = this.readString();
        String name = this.readString();
        String descriptor = this.readString();
        String sIsInterface = this.readUntil(')');
        boolean isInterface;
        if (sIsInterface.equalsIgnoreCase("true")) isInterface = true;
        else if (sIsInterface.equalsIgnoreCase("false")) isInterface = false;
        else throw new IllegalStateException("Expected boolean but got '" + sIsInterface + "'");
        this.ensureNext(' ', true);
        this.skip();

        return new Handle(opcode, owner, name, descriptor, isInterface);
    }

    public Object readConstantPoolEntry() {
        if (this.canReadInt(true)) return this.readInt();
        else if (this.canReadFloat(true)) return this.readFloat();
        else if (this.canReadLong(true)) return this.readLong();
        else if (this.canReadDouble(true)) return this.readDouble();
        else if (this.peekString().toLowerCase(Locale.ROOT).startsWith("type(")) return this.readType();
        else if (this.peekString().toLowerCase(Locale.ROOT).startsWith("handle(")) return this.readHandle();

        try {
            return this.readInt();
        } catch (Throwable t) {
            return this.readString();
        }
    }

}
