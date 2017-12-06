#C#的通用优化手段

[TOC]

### 1. 应尽量减少创建C#堆内存对象

建议使用成员变量，或者Pool来规避高频创建C#堆内存对象的创建。而且堆内存对象创建本身就是个相对较慢的过程。

### 2. 应为struct对象重载所有object函数

为了普适性，C#的struct的默认Equals()、GetHashCode()和ToString()都是较慢实现，甚至涉及反射。用户自定义的struct，都应重载上述3个函数，手动实现，比如：

~~~C#
public struct NetworkPredictId{
    int m_value;
    public override int GetHashCode(){
        return m_value;
    }
    public override bool Equals(object obj){
        return obj is NetworkPredictId && this == (NetworkPredictId)obj;
    }
    public override string ToString(){
        return m_value.ToString();
    }
    public static bool operator ==(NetworkPredictId c1, NetworkPredictId c2){
        return c1.m_value == c2.m_value;
    }
    public static bool operator !=(NetworkPredictId c1, NetworkPredictId c2){
        return c1.m_value != c2.m_value;
    }
}
~~~



### 3. 如果可能，尽量用Queue/Stack来代替List

一般习惯用List来实现数据集合的需求。但好一些情况下，事实上是不需对其进行随机访问，而仅仅是“增加”、“删除”操作。此时，应该使用增删复杂度都是O(1)的Queue或者Stack。

### 4. 注意List常用接口复杂度

Add()常为O(1)复杂度，但超过Capacity时，为O(n)复杂度。所以应注意合理地设置容器的初始化Capacity。
Insert()为O(n)复杂度。
Remove()为O(n)复杂度。RemoveAt(index)为O(n)复杂度，n=(Count - index)。故建议移除时应优先从尾部移除。当批量移除时，亦指出RemoveRange提高移除效率。

~~~C#
/// remove not exsiting items in O(n)
int oldCount = m_items.Count;
int newCount = 0;
Item oneItem;
for(int i = 0; i < oldCount; ++i){
    oneItem = m_items[i];
    if(CheckExisting(oneItem)){
        m_items[newCount] = oneItem;
        ++newCount;
    }
}

m_items.RemoveRange(newCount, removeCount);
~~~

### 5. 应注意容器的初始化capacity

同理如上条目。另，Capacity增长时，除了O(n)的复杂度，也有GC消耗。

### 6. 减少Dictionary的冗余访问

程序员习惯编写这样的代码：

~~~C#
if(myDictionary.Contains(oneKey))
{
    MyValue myValue = myDictionary[oneKey];
   // ...
}
~~~

但其可减少冗余的哈希次数，优化为：

~~~C#
MyValue myValue;
if(myDictionary.TryGetValue(oneKey, out myValue))
{
    // ...
}
~~~

### 7. 避免使用foreach()

因为它会调用GetEnumerator()，从而在循环的过程中在堆中产生enumerator对象，而这些对象并无他用，所以应当使用传统的for函数来完成工作以避免额外的内存负担。

### 8. 避免使用strings

在.NET中strings是不可变长并且是在堆中申请的。而且不能像在C语言中的方式一样去修改它。对于UI，使用StringBuilder来创建strings是一种比较有效率的做法，而且应当在尽量晚的时候才进行转换。这并不影响你使用作为关键字索引来使用，因为游标是会找到内存中的实例所在，但是请避免过多的修改它。

### 9. 使用structs

在mono中structs是在栈中申请的。如果有一个工具类同时仅仅是在局部范围使用的，那么就可以考虑把它变成struct。要记住structs是传值的，所以需要通过引用来避免额外的拷贝开销。

### 10. 使用structs来代替运行范围内的固定数组

如果类的方法中有一些固定大小的数组，那么不妨使用structs或者成员数组来代替它，这样不必每次运行函数都申请变量，尤其是如果这些方法需要被调用成百上千遍。
把引用List作为参数传入而不是新创建一个 ： 仿佛没有节约什么，因为传入的List同样需要申请内存对吗？但这么做的原因是为了下一个看上去并不漂亮的优化。

### 11. 使用成员变量来代替高频率出现的局部变量

如果函数每次运行时都需要一个很大的List，那么不妨将这个List设为成员变量，这样List可以独立于函数运行，在需要时你可以通过Clear()函数来清空List，而在C#中Clear()并不会真正的将内存空间删除，虽然会有碍于代码的美观，但是可以极大减轻性能上的负担。

### 12. 避免IEnumerable扩展函数

无可厚非IEnumerable扩展函数虽然使用方便，但是却会带来更多的内存负担。所以和foreach()同样道理，应当尽力避免在代码中使用IEnumerable<>接口，可以用IList<>来代替。

### 13. 减少使用函数指针/delegate

使用委托或者Func<>会为程序带来新的内存分配，可是实际上却也找不到更好的办法来实现同样的功能，尤其是它会为程序在解耦带来巨大的好处，但是总而言之要尽量精简。

### 14. 要注意cloned材质

如果想获取renderer的material属性，那么需要注意即使不想修改任何东西，系统也会复制一份material，而且这个material不是自动回收的，只有在切换场景或者手动调用Resources.UnloadUnusedAssets()才会被释放。如果你不需要修改材质，通过myRenderer.sharedMaterial来访问材质吧。