package Network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class SyncList
{
	public ArrayList<Integer> list = new ArrayList<Integer>();
	public ArrayList<Integer> addList = new ArrayList<Integer>();
	public ArrayList<Integer> removeList = new ArrayList<Integer>();
	
	public String mName;
	
	public SyncList(String name)
	{
		mName = name;
	}
	
	public ArrayList<Integer> GetList()
	{
		ArrayList<Integer> newList = new ArrayList<Integer>();
		newList.addAll(list);
		
		newList.addAll(addList);
		newList.removeAll(removeList);
		
		Set<Integer> noDuplicates = new TreeSet<Integer>();
		
		noDuplicates.addAll(newList);
		
		ArrayList<Integer> noDups = new ArrayList<Integer>();
		noDups.addAll(noDuplicates);
		noDups.removeAll(removeList);
		
		return noDups;
	}
	
	public void AddToList(ArrayList<Integer> items)
	{
		addList.addAll(items);
	}
	
	public void RemoveFromList(ArrayList<Integer> items)
	{
		removeList.addAll(items);
	}
	
	public void Collapse()
	{
		list.addAll(addList);
		addList.clear();
		list.removeAll(removeList);
		removeList.clear();
	}
	
	public void ListToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(list.size());
		for (int i = 0; i < list.size(); ++i)
		{
			stream.writeInt(list.get(i));
		}
		
		stream.writeInt(addList.size());
		for (int i = 0; i < addList.size(); ++i)
		{
			stream.writeInt(addList.get(i));
		}
		
		stream.writeInt(removeList.size());
		for (int i = 0; i < removeList.size(); ++i)
		{
			stream.writeInt(removeList.get(i));
		}
	}
	
	public void BinaryToList(DataInputStream stream) throws IOException
	{
		int size = stream.readInt();
		list = new ArrayList<Integer>();
		for (int i =0; i < size; ++i)
		{
			list.add(stream.readInt());
		}
		
		size = stream.readInt();
		addList = new ArrayList<Integer>();
		for (int i =0; i < size; ++i)
		{
			addList.add(stream.readInt());
		}
		
		size = stream.readInt();
		removeList = new ArrayList<Integer>();
		for (int i =0; i < size; ++i)
		{
			removeList.add(stream.readInt());
		}
		
		Set<Integer> noDuplicates = new TreeSet<Integer>();
		
		noDuplicates.addAll(list);
		list.clear();
		list.addAll(noDuplicates);
	}
	

}
