package com.hoddmimes.te.messages;

import com.hoddmimes.jsontransform.MessageInterface;

import java.util.Optional;

public interface EngineMsgInterface extends MessageInterface
{
	public Optional<String> getSymbol();
	public Optional<String> getRef();
}
