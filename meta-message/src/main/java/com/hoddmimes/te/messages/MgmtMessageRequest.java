package com.hoddmimes.te.messages;

import com.hoddmimes.jsontransform.MessageInterface;

import java.util.Optional;

public interface MgmtMessageRequest extends MessageInterface
{
	public Optional<String> getRef();
}
