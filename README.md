##BlurView##
这次就不吹牛逼了

### Support ###
    
    1 BlurBehindView 三种更新方式:只模糊一次(Never); 滚动时实时更新（scroll）; 无条件实时更新(Continuously); 
    2 BlurDrawable 实时模糊Drawable
    3 多种Blur方式，核心算法源自 https://github.com/patrickfav/Dali
    

### Example ###

[Download demo.apk](https://github.com/robinxdroid/BlurView/blob/master/app-debug.apk?raw=true)

### Screenshot ###

![](https://github.com/robinxdroid/BlurView/blob/master/1.png?raw=true)
![](https://github.com/robinxdroid/BlurView/blob/master/BlurBehindView.gif?raw=true) ![](https://github.com/robinxdroid/BlurView/blob/master/BlurBehindView1.gif?raw=true)
![](https://github.com/robinxdroid/BlurView/blob/master/BlurDrawable.gif?raw=true) 

### Usage ###
Gradle:
```java
    compile 'net.robinx:lib.blurview:1.0.2'
```
```java
defaultConfig {
        ....
       
        renderscriptTargetApi 19
        renderscriptSupportModeEnabled  true
    }
```
**Blur:**

```java
blurBitmap = RSGaussianBlurProcessor.getInstance(context).process(originalBitmap, blurRadius); //RenderScript其中一个方式(此方式在所有方式中速度最快)

blurBitmap = NdkStackBlurProcessor.INSTANCE.process(originalBitmap, blurRadius);  //NDK方式,速度比上面的方式略慢，相对稳定

blurBitmap = BlurProcessorProxy.INSTANCE  //代理
                        .processor(processor) //传入Processor对象,eg:NdkStackBlurProcessor.INSTANCE
                        .copy(true) //为true时，将copy一份，不影响原图
                        .process(originalBitmap, blurRadius);

```
更多请见[BlurActivity.java](https://github.com/robinxdroid/BlurView/blob/master/app/src/main/java/net/robinx/blur/view/BlurActivity.java)

**BlurDrawable**

扩展Drawable可设置为任何View背景

```java
 BlurDrawable blurDrawable = new BlurDrawable(bluredview);
 blurDrawable.drawableContainerId(R.id.blur_drawable_container) //此方法用于bluredview内部包含了将要设置blurDrawable的View的时候
            .cornerRadius(10) //圆角
            .blurRadius(10) //Blur程度 <= 25
            .overlayColor(Color.parseColor("#64ffffff")) //覆盖颜色
            .offset(mBlurDrawableRelativeLayout.getLeft(), mBlurDrawableRelativeLayout.getTop() ); //画布偏移
```   

**BlurBehindView**：

 1.XML:

```java
<net.robinx.lib.blurview.BlurBehindView
        android:id="@+id/blur_behind_view"
        android:layout_width="150dp"
        android:layout_height="150dp">
</net.robinx.lib.blurview.BlurBehindView>
```   
2.代码中使用: 
```java
BlurBehindView blurBehindView = (BlurBehindView) findViewById(R.id.blur_behind_view);
blurBehindView.updateMode(BlurBehindView.UPDATE_CONTINOUSLY) //更新方式，3种，见demo
        .blurRadius(8)  //模糊程度，RenderScript方式时，<= 25
        .sizeDivider(10) //对原图的缩放程度，此值越大，缩放程度越大，Blur时间越短
        .clipPath(path) //裁剪路径，传入不同的path可裁成不同的形状
        .clipCircleOutline(true) //是否裁成圆形
        .clipCircleRadius(1.0f) //圆形半径系数 <= 1.0
        .cornerRadius(10) //圆角
        .processor(NdkStackBlurProcessor.INSTANCE); //BlurProcessor，内置了很多不同的Processor，可自己定义，默认RenderScript进行处理
``` 
**自定义Processor**：

实现BlurProcessor接口，实现process(Bitmap original, int radius)函数即可
 
 
**Thanks**：

[https://github.com/patrickfav/Dali](https://github.com/patrickfav/Dali)<br>
[https://github.com/kikoso/android-stackblur](https://github.com/kikoso/android-stackblur)

#About me
Email:735506404@robinx.net<br>
Blog:[www.robinx.net](http://www.robinx.net)

