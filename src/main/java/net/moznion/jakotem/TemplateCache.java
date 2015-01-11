package net.moznion.jakotem;

public interface TemplateCache {
	public OpcodeSequence get(String filePath);
	public void set(String filePath, OpcodeSequence irep);
}
