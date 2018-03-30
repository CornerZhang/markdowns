# TickProfiler的设计和使用

[TOC]

###动机

缺少一个针对Unity输出Release版本的运行时性能分析工具，以此获得最终产品的性能参数，有助于尽早确定性能的量化标准，而不是Development版本中获得的模糊的相对性能参数。

### 设计要求

- 剖析器本身就是极低的负载，只做采样后的内存记录

- 达到简单易用的效果

- 输出的信息最好能达到可视化，能转成图表

- 其内部实现只会使用有限的C#语言的特性，太过高级的就怕开il2cpp后没法转化


### 实现原理

使用UnityEngine.Time.realtimeSinceStartup获得高精度时间值，同时保留下来为last；然后后面再取一次与last做差，存储为delta。这个delta就是每一帧上一个代码段对应的时间成本消耗，需要保留下来。由于每一帧上都保存了这样的delta到一个容器列表里，没有其它开销。最后与程序转化场景或是结束时dump下所有的采样值，并且以有利于处理成可视信息格式的csv文件持久化到外部存储器。之后由Excel卡开，做进一步的加工。

### 用法

项目中需要如下这样的写代码

```C#
public class Game_Controller: MonoBehaviour
{
    private TickProfiler.TickRecorder tickUpdateFunction;	// 时间采样器
    private bool beWritten;	// 防止重复Dump文件
    //...
    
    void Start() {
        tickUpdateFunction = TickPrfiler.CreateRecorder("UpdateFunction");
        beWritten = false;
    }
    
    //...
    
    void Update() {
        tickUpdateFunction.BeginTimeTick();
        //...一些代码
        tickUpdateFunction.EndTimeTick();
    }
    
    private void OnApplicationPause(bool pause)
    {
        if( pause && !beWritten ) {
            TickProfiler.DumpAllTicks("perf_log.csv");
            beWritten = true;
        }

    }
    
    private void OnApplicationQuit()
    {
#if UNITY_EDITOR
        TickProfiler.DumpAllTicks("perf_log.csv");
#endif
    }
}
```

然后输出Release版（Build界面里去除Development选项）到Android、iOS上运行，当然后把输出到app沙箱目录(看Unity文档里有关Application.persistentDataPath部分)里的perf_log.csv取出到PC机上，用Excel 2016处理就是。

(完)