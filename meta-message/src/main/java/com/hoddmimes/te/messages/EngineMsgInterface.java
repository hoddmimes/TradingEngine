package com.hoddmimes.te.messages;

import com.hoddmimes.jsontransform.MessageInterface;

import java.util.Optional;

public interface EngineMsgInterface extends RequestMsgInterface
{
	public Optional<String> getSid();
}
