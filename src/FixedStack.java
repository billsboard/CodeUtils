import java.util.ArrayList;

class FixedStack<T> {
    private ArrayList<T> stack;
    private int size;

    FixedStack(int size)
    {
        this.stack = new ArrayList<>();
        this.size = size;
    }

    void push(T obj)
    {
        if (stack.size() >= size)
            stack.remove(0);

        stack.add(obj);
    }

    T element(){
        return stack.get(stack.size() - 1);
    }

    T pop(){
        T element = stack.get(stack.size() - 1);
        stack.remove(stack.size() -1);
        return element;
    }

    int size()
    {
        return size;
    }

}