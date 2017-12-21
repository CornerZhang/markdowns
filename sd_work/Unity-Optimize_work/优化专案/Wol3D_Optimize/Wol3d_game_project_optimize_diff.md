#《传世3D》游戏优化

![wol3d_intro](wol3d_intro.png)



对公司的现有项目《传世３Ｄ》做性能优化，主要集中在了ＣＰＵ的后期效果优化和内存优化方面。

最为直观有效的说明就是，对优化前后的性能参数的比对

### 优化前

![beforeMix](beforeMix.png)

### 优化后

![after_mix](after_mix.png)



前后最明显的差异就在

![after_args](after_args.png)

其每一次Camera.Render相比没做优化前的

![before_args](before_args.png)

有的极大的节省，虽说没有看到的有一个数量级的节省，其实已经有极大的改善。

Overhead也从占用总开销的3.6%降到1.3%，这也说明ＣＰＵ的使用得到了更有效的改进，许多的冗余代码从中得以剔出。



这样的结果，来自多次优化工作的迭代，每一次迭代

来自于

