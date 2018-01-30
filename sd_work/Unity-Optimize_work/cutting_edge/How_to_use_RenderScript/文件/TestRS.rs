#pragma version(1)                                  //RenderScript的版本，目前为1
#pragma rs java_package_name(com.example.rs)       //该RS脚本之后生成的ScriptC_文件名.java所在的包，括号内的内容可以自己定义

//可以实现色反的函数

uchar4 __attribute__((kernel)) invert(uchar4 in)
{
  uchar4 out = in;
  out.r =255- in.r;
  out.g = 255-in.g;
  out.b = 255-in.b;
  return out;

}