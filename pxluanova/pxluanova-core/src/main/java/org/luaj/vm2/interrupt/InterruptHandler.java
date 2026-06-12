package org.luaj.vm2.interrupt;

import org.luaj.vm2.LuaError;

public interface InterruptHandler {
	InterruptAction interrupted() throws LuaError;
}
