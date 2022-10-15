package org.spongepowered.asm.mixin.injection.callback;

public class CallbackInfoReturnable<R> extends CallbackInfo {

    CallbackInfoReturnable() {
    }

    public void setReturnValue(R returnValue) {
    }

    public R getReturnValue() {
        return null;
    }

    public byte getReturnValueB() {
        return 0;
    }

    public char getReturnValueC() {
        return '\0';
    }

    public double getReturnValueD() {
        return 0;
    }

    public float getReturnValueF() {
        return 0;
    }

    public int getReturnValueI() {
        return 0;
    }

    public long getReturnValueJ() {
        return 0;
    }

    public short getReturnValueS() {
        return 0;
    }

    public boolean getReturnValueZ() {
        return false;
    }

}
