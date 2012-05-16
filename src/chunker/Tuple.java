package chunker;

public class Tuple<T,E>
{
	private T fst = null;
	private E snd = null;

	public Tuple(T fst, E snd)
	{
		this.fst = fst;
		this.snd = snd;
	}

	public T fst()
	{
		return fst;
	}

	public void setFst(T fst)
	{
		this.fst = fst;
	}

	public E snd()
	{
		return snd;
	}

	public void setSnd(E snd)
	{
		this.snd = snd;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Tuple)
		{
			Tuple t = (Tuple)o;
			return fst.equals(t.fst) && snd.equals(t.snd);
		}
		else
			return false;
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	@Override
	public String toString()
	{
		return "<" + fst.toString() + ", " + snd.toString() + ">";
	}
}
