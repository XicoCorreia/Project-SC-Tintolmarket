package com.segc;

import java.io.Serializable;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class Message implements Serializable {

    private static final long serialVersionUID = -7586386340771570949L;
    private final String author;
    private final byte[] content;

    /**
     * Creates a new {@code Message} with the given author and content.
     *
     * @param author  the client ID of the author of this message
     * @param content the content of this message
     */
    public Message(String author, byte[] content) {
        this.author = author;
        this.content = content;
    }

    public String getAuthor() {
		return author;
	}

	public byte[] getContent() {
		return content;
	}

	@Override
    public String toString() {
        return "Enviado por: '" + author + "'" + System.lineSeparator() + content;
    }
}
