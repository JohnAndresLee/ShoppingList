## ShoppingList

### 问题

购物篮分析问题：给定一个交易集（其中每个交易是一个商品集），请编写MapReduce程序找出哪几个商品（k）经常会一起购买（sup>=min）。要求1：用伪代码描述程序设计思想；要求2：提交源程序和运行结果（样例的结果，完整数据集的结果），要求给出不同参数的输出，至少2组以上。

### 输入&输出

- 输入：输入数据为文本文件，每一行为一次交易的商品列表。商品之间用空格分隔。
- 参数：k为频繁项集中的商品数量，默认为2；min为最小支持度，默认为2。
- 输出：找出的k频繁项集，并按照feq从大到小排序，格式为：(I1,I2...Ik),feq

### 问题分析

本次实验的主要思想与第一次的wordcount项目非常类似，代码框架和逻辑基本一致（按出现频率排序和格式化输出模块）。主要的区别在于key的生成，wordcount中的key由年份和单词信息构成，本项目中的key则为每个购物清单中长度由用户输入给定的子集合，故可以考虑用字符串或者是list等容器动态存储。

```pseudocode
class Mapper
	method Map(shoppinglist)
		k_sets ← combinations(shoppinglist)
		for all k_set in k_sets
			Emit(k_set, one)
		
class Reducer
	method Reduce(k_set, values(one,one,...))
		count = 0
		for all value in values
			count+=1
		Emit(k_set, count)
```

### k频繁项集的生成

用递归的思路完成本部分的代码设计：

```java
public List<List<String>> combinations(List<String> c, int ke){
    if(ke>c.size()){
        return new ArrayList<List<String>>();
    }
    List<List<String>> ans = new ArrayList<List<String>>();
    if(ke==1){
        for(String ci:c){
            List<String> ttt = new ArrayList<String>();
            ttt.add(ci);
            ans.add(ttt);
        }
        return ans;
    }
    String first = c.get(0);
    List<String> c_copy = new ArrayList<String>();
    c_copy.addAll(c);
    c_copy.remove(0);
    List<List<String>> tmp = combinations(c_copy,ke-1);
    for(List<String> t:tmp){
        t.add(0, first);
    }
    ans.addAll(tmp);
    List<List<String>> tmp2 = combinations(c_copy, ke);
    ans.addAll(tmp2);
    return ans;
}
```

### 用户输入的获取

在main函数中定义并获取用户命令行的输入：

```java
GenericOptionsParser optionParser = new GenericOptionsParser(conf, args);
String[] remainingArgs = optionParser.getRemainingArgs();
List<String> otherArgs = new ArrayList<String>();
for (int i=0; i < remainingArgs.length; ++i) {
    otherArgs.add(remainingArgs[i]);
}

job.getConfiguration().setInt("k", Integer.parseInt(otherArgs.get(0)));
job.getConfiguration().setInt("min", Integer.parseInt(otherArgs.get(1)));
```

根据使用需求可以在map或者reduce类的setup函数中获取输入参数

```java
public void setup(Context context) throws IOException, InterruptedException {
    Configuration conf = context.getConfiguration();
    k = conf.getInt("k", 2);
}
```

### 实验结果

实验要求给出至少2组以上不同参数的输出，

k=2, min=2部分结果展示，全部结果保存在../output/hw7_out_2_2.txt中

![image-20221109123711342](https://cdn.jsdelivr.net/gh/JohnAndresLee/websitepicture/image-20221109123711342.png)

k=3, min=20部分结果展示，全部结果保存在../output/hw7_out_3_20.txt中

![image-20221109123616143](https://cdn.jsdelivr.net/gh/JohnAndresLee/websitepicture/image-20221109123616143.png)