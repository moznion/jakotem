package net.moznion.jakotem;

import java.util.List;

public interface Syntax {
	List<Token> tokenize(Source source, String src);

	Node parse(Source source, List<Token> tokens) throws IllegalSyntaxException;

	OpcodeSequence compile(Source source, Node ast) throws IllegalSyntaxException;
}
