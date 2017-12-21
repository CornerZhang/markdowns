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

### 15. 避免频繁地SetActive操作

避免频繁地SetActive操作，由于SetActive本身也有一定消耗，而且一些特殊的组件类似于：Text、MaskGraphic等，在OnEnable与OnDisable时有较为明显的消耗，建议在频繁进行SetActive的操作时采用先移出屏幕等待一段时间之后再将物体隐藏，保证不过度频繁地将物体重复Active或者Inactive。而在一些不适用于移出屏幕的物体，类似于UI，考虑减少该类操作，或者使用将Text设为空或者透明度设为0来避免调用OnEnable与OnDisable操作。

### 16. 使用gameObject.CompareTag(“XXX”)而非gameObject.tag

函数调用gameObject.tag会造成预想不到的堆内存分配，这个函数会将结果存为新的字符串返回，这就会造成不必要的内存垃圾，对结果进行缓存是一种有效的办法，但是在Unity中都对应的有相关的函数来替代。对于比较gameObject.tag，可以采用GameObject**.**CompareTag()来替代。

###17. 多使用内建的常量 

使用内建的常量，例如Vector3.zero、Vector3.up等等，避免频繁创建相同的对象。

### 18. 对于一些简单的协程，建议用自己的工具类实现

自己可以写一个工具类，使用Update替代简单的协程，例如等待若干秒等等，可以消除创建协程的消耗。

### 19. 在UI中使用池时考虑使用分级机制

在池中越不频繁出现的UI就应该更快地被销毁以释放内存，而频繁出现的UI则等待更长的时间。

### 20. 在使用池时需要注意的事项

在使用池进行内存管理时特别要注意，当一个物体你不再需要的时候，请将其置为null。例如你封装了一个数组，其中装入了许多的对象，当你移除一个对象的时候或许并没有将其真正置空，而是移动了目前指向的位置，那么你本应移除的对象就泄漏了出去。

### 21. 不要主动调用GC

不要主动调用GC。而是通过良好的代码，及时去除不需要的对象的引用可以更好地让我们使用GC来回收垃圾。

### 22. 使用消耗更小的运算

使用消耗更小的运算：例如1/5使用1*0.2来代替、用位运算代替简单乘除。（不过在性能并不是非常非常敏感的地方可以忽略位运算这一条，毕竟可读性还是要的。)

### 23. 使用延迟加载的方式

使用延迟加载的方式，一些不常用的资源在第一次使用的时候再进行加载。

### 24. 垃圾最小化

垃圾回收器对内存管理表现的非常出色，并且它以非常高效的方法移除不再使用的对象。但不管怎样，申请和释放一个基于堆内存的对象总比申请和释放一个不基于堆内存的对象要花上更多的处理器时间。所以，要避免创建大量的对象，也不要创建你不使用的对象。同时避免在局部函数上多次创建引用对象。相反，把局部变量提供为类型成员变量，或者把你最常用的对象实例创建为静态对象。最后，考虑使用可变对象创建器来构造恒定对象。

### 25 装箱和拆箱的最小化

.Net框架使用了装箱与拆箱来链接两种不同类型的数据。装箱和拆箱可以让你在须要使用System.Object对象的地方使用值类型数据。但装箱与拆箱操作却是性能的强盗，在些时候装箱与拆箱会产生一些临时对象，它会导致程序存在一些隐藏的BUG。尽量使用泛型容器避免不必要的装箱拆箱。

### 26. 避免返回内部类对象的引用

所谓的只读属性就是指调用者无法修改这个属性。不幸的是，这并不是一直有效的。如果创建了一个属性，它返回一个引用类型，那么调用者就可以访问这个对象的公共成员，也包括修改这些属性的状态。所以要避免返回内部类对象的引用。

### 27. 避免转换操作

当你为某个类型添加转换操作时，就等于是告诉编译器：你的类型可以被目标类所取代。这可能会引发一些潜在的错误，因为你的类型很可能并不能被目标类型所取代。它的副作用就是修改了目标类型的状态后可能对原类型根本无效。更糟糕的是，如果你的转换产生了临时对象，那么副作用就是你直接修改了临时对象，而且它会永久丢失在垃圾回收器中。总之，使用转换操作应该基于编译时的类型对象，而不是运行时的类型对象。

###28. 选择小而简单的函数

.Net运行时调用JIT编译器，用来把由C#编译器生成的IL指令编译成机器代码。JIT编译器是在须要时，以每个函数为单元生成机器指令(当内联调用时，或者是一组方法)。小函数可以让它非常容易被JIT编译器分期处理。小函数更有可能成为内联候选对象。当然并不是足够小才行：简单的控制流程也是很重要的。函数内简单的控制分支可以让JIT以容易的寄存变量。

### 29. 选择小而内聚的程序集

把所有的东西，都放到一个程序集，这不利于重用其中的组件，也不利于系统中小部份的更新。很多以二进制组件形式存在的小程序集可以让这些都变得简单。

### 30. 减少UnityEngine.Object的null比较

因为Unity overwrite了Object.Equals()，unityEngineObject==null事实上和GetComponent()的消耗类似，都涉及到Engine层面的机制调用，所以UnityEngine.Object的null比较，都会有少许的性能消耗。对于基础功能、调用栈叶子节点逻辑、高频功能，我们应少null比较，使用assertion来处理。只有在调用栈根节点逻辑，有必要的时候，才进行null比较。

### 31. 减少GetComponent()的频率

即使在Unity5.5中，GetComponent()会有一定的GC产生，有少量的CPU消耗。如有可能，我们依然需要规避冗余的GetComponent()。另，自Unity5起，Unity已就.transform进行了cache，我们不需再为.transform担心。

### 32. 减少每帧Material.GetXX()/Material.SetXX()的次数

每次调用Material.GetXX()或Material.SetXX()都会有消耗，应减少调用该API的频率。比如使用C#对象变量来记录Material的变量状态，从而规避Material.GetXX()；在Shader里把多个uniform half变量合并为uniform half 4，从而把4个Material.SetXX()调用合并为1个Material.SetXX()。

### 33. 使用支持Conditional的日志输出机制

简单使用Debug.Log(ToString() + "hello " + "world");，其实参会造成CPU消耗及GC。使用支持Conditional的日志输出机制，则无此问题，只需在构建时，取消对应的编译参数即可。

### 34. 减少不必要的Transform.position/rotation等访问

每次访问Transform.position/rotation都有相应的消耗，应能cache就cache其返回结果。可以在start函数中预先存起来。

### 35. 涉及物理运算时使用Layer而不是Tag

我们可以轻松为对象分配层次和标签，并查询特定对象，但是涉及碰撞逻辑时，层次至少在运行表现上会更有明显优势。更快的物理计算和更少的无用分配内存是使用层次的基本原因。

### 36. 删除空的Update方法

当通过Assets目录创建新的脚本时，脚本里会包括一个Update方法，当你不使用时删除它。

### 37. 不要滥用switch

为你的设计找一个平衡点，不要滥用switch（包括数组容器或者子类设计）导致复杂度的上升，同时，减少嵌入回调等代码的可能，滥用回调，有可能导致设计框架崩溃，需要重新设计或者项目崩溃。

### 38. 过大的运算请分帧处理

如果你的一次运算量过大，并且不要求在当前帧完成，请分帧进行处理，对每一帧进行迭代运算，不要把所有运算放在同一帧（例如，寻路算法，遗传算法等等）。

### 39. 减少函数引用

函数的引用，无论是指向匿名函数还是显式函数，在unity中都是引用类型变量，这都会在堆内存上进行分配。匿名函数的调用完成后都会增加内存的使用和堆内存的分配。具体函数的引用和终止都取决于操作平台和编译器设置，但是如果想减少GC最好减少函数的引用。

###40. 重构代码来减小GC的影响

即使我们减小了代码在堆内存上的分配操作，代码也会增加GC的工作量。最常见的增加GC工作量的方式是让其检查它不必检查的对象。通过重构代码，我们可以返回实体的标记，而不是实体本身，这样就没有多余的object引用，从而减少GC的工作量。

### 41. GetType() 会产生 GC Alloc

GetType() 会产生GC Alloc (每个调用 20 Bytes)。

###42. 在针对 GC Alloc 做优化时，对象数量 > 引用关系复杂度 > 对象尺寸 

对 Boehm garbage collector 而言，对象数量直接影响单次 GC 的时间开销 每个对象 90 个时钟周期左右 (大量时间是 cache-missing 所致) 算下来每秒 15M 数目的对象，也就是每毫秒标记 15000 个左右。

###43. 使用整数句柄来代替引用

当可以使用整数句柄来代替引用时，尽量使用整数句柄 (简化引用关系)。

###44. 避免频繁调用分配内存的 accessors

避免频繁调用分配内存的 accessors (如 .vertices/.normals/.uvs/.bones)。

###45. 避免频繁调用Int.ToString() 及其它类型的衍生

避免频繁调用Int.ToString() 及其它类型的衍生。

###46. 避免频繁使用 Mathf.Max 等函数的数组版

避免频繁使用 Mathf.Max 等函数的数组版（多于两个参数都会调到数组版），所有具有 params 修饰的函数都应避免频繁使用（以避免临时数组的分配）。

###47. 在不需要时避免使用 GUILayout - OnGUI 时把 useGUILayout关掉

在不需要时避免使用 GUILayout - OnGUI 时把 useGUILayout关掉。

###48. 在使用协程 yield 时尽量复用 WaitXXX对象

在使用协程 yield 时尽量复用 WaitXXX对象 (使用Yielders.cs) 而不是每次分配。

###49. 避免在Update() 内 FindObjectsOfType()

避免在Update() 内 FindObjectsOfType()。

###50. 避免在Update() 里赋值给栈上的数组

避免在Update() 里赋值给栈上的数组，会触发堆内的反复分配。