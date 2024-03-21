package uk.co.eidolon.tact2.AIHelpers;

public abstract class IAIThread extends Thread
{

	public abstract void run();

	public boolean stop = false;
	public boolean stopped = false;
}