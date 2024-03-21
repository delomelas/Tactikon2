package uk.co.eidolon.shared.network;

abstract public class ISendEvents
{
	abstract public void preExecute();
	abstract public void postExecute();
	abstract public void postExecuteBackground();
}
