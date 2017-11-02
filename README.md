# Rexxar Android


[![Test Status](https://travis-ci.org/douban/rexxar-android.svg?branch=master)](https://travis-ci.org/douban/rexxar-android)
[![IDE](https://img.shields.io/badge/Android-Studio-blue.svg)]()
[![Android](https://img.shields.io/badge/Android-4.0-green.svg)]()
[![Language](https://img.shields.io/badge/language-Java-blue.svg)]()


**Rexxar** 是一个针对移动端的混合开发框架。现在支持 Android 和 iOS 平台。`rexxar-android` 是 Rexxar 在 Android 系统上的客户端实现。

通过 Rexxar，你可以使用包括 javascript，css，html 在内的传统前端技术开发移动应用。Rexxar 的客户端实现 Rexxar Container 对于 Web 端使用何种技术并无要求。我们现在的 Rexxar 的前端实现 Rexxar Web，以及 Rexxar Container 在两个平台的实现 rexxar-ios 和 rexxar-android 项目中所带的 Demo 都使用了 [React](https://facebook.github.io/react/)。但你完全可以选择自己的前端框架在 Rexxar Container 中进行开发。

rexxar-android 现在支持 Android 4.0 及以上版本。

## Rexxar 简介

关于 Rexxar 的整体介绍，可以看看这篇博客：[豆瓣的混合开发框架 -- Rexxar](http://lincode.github.io/Rexxar-OpenSource)。

Rexxar 包含三个库：

- Rexxar Web ：[https://github.com/douban/rexxar-web](https://github.com/douban/rexxar-web)。

- Rexxar Android：[https://github.com/douban/rexxar-android](https://github.com/douban/rexxar-android)。

- Rexxar iOS：[https://github.com/douban/rexxar-ios](https://github.com/douban/rexxar-ios)。

## 使用

你可以查看 Demo 中的例子。了解如何使用 Rexxar。Demo 给出了完善的示例。

Demo 中使用 github 的 raw 文件服务提供一个简单的路由表文件 routes.json，demo.html 以及相关 javascript 资源的访问服务。在你的线上服务中，当然会需要一个真正的生产环境，以应付更大规模的路由表文件，以及 javascript，css，html 资源文件的访问。你可以使用任何服务端框架。Rexxar 对服务端框架并无要求。

### 安装

#### gradle

```groovy
   compile 'com.douban.rexxar:core:0.3.7'
```


### 配置

#### 1. 初始化

在Application的`onCreate`中调用

```Java
  Rexxar.initialize(Context context);
```

#### 2. 设置路由表文件 api：

```Java
  RouteManager.getInstance().setRouteApi("https://raw.githubusercontent.com/douban/rexxar-web/master/example/dist/routes.json");
```

Rexxar 使用 uri 来标识页面，提供一个正确的 uri 就可以打开对应的页面，路由表提供了每个 uri 对应的 html 资源的下载地址。

Demo 中的路由表如下：

```json

{
  "items": [
    {
      "deploy_time": "Sun, 09 Oct 2016 05:54:22 GMT",
      "remote_file": "https://raw.githubusercontent.com/douban/rexxar-web/master/example/dist/rexxar/demo-252452ae58.html",
      "uri": "douban://douban.com/rexxar_demo[/]?.*"
    }
  ],
  "partial_items": [
    {
      "deploy_time": "Sun, 09 Oct 2016 05:54:22 GMT",
      "remote_file": "https://raw.githubusercontent.com/douban/rexxar-web/master/example/dist/rexxar/demo-252452ae58.html",
      "uri": "douban://partial.douban.com/rexxar_demo/_.*"
    }
  ],
  "deploy_time": "Sun, 09 Oct 2016 05:54:22 GMT"
}


```

#### 3. 设置需要代理或缓存的请求host

```Java
  ResourceProxy.getInstance().addProxyHosts(List<>() hosts);
```

Rexxar是通过`WebViewClient`的`shouldInterceptRequest`方法来拦拦截请求，请求线上数据并返回给'webview'。为了减少不必要的流程破坏，只有明确需要拦截的hosts（支持正则）的请求才会被拦截代理，并根据mime-type决定哪些内容需要缓存。

#### 4. 预置资源文件

使用 Rexxar 一般会预置一份路由表，以及资源文件在应用包中。这样就可以减少用户的下载，加快第一次打开页面的速度。在没有网络的情况下，如果没有数据请求的话，页面也可访问。这都有利于用户体验。
预置文件路径是`assets/rexxar`, 暂不支持修改。



### 使用 RexxarWebView

你可以直接使用 `RexxarWebView` 作为你的混合开发客户端容器。或者你也可以在 `RexxarWebView` 基础上实现你自己的客户端容器。

为了初始化 RexxarWebView，你需要只一个 url。在路由表文件 api 提供的路由表中可以找到这个 url。这个 url 标识了该页面所需使用的资源文件的位置。Rexxar Container 会通过 url 在路由表中寻找对应的 javascript，css，html 资源文件。

```Java
  // 根据uri打开指定的web页面
  mWebView.loadUri("douban://douban.com/rexxar_demo");
```

## 定制你自己的 Rexxar Container

我们暴露了三类接口。供开发者更方便地扩展属于自己的特定功能实现。

### 定制 RexxarWidget

Rexxar Container 提供了一些原生 UI 组件，供 Rexxar Web 使用。RexxarWidget 是一个 Java 协议（Protocol）。该协议是对这类原生 UI 组件的抽象。如果，你需要实现某些原生 UI 组件，例如，弹出一个 Toast，或者添加原生效果的下拉刷新，你就可以实现一个符合 RexxarWidget 协议的类，并实现以下方法：`getPath:`, `handle:`。

在 Demo 中可以找到一个例子：`TitleWidget` ，通过它可以设置导航栏的标题文字。

```Java

    public class TitleWidget implements RexxarWidget {

    static final String KEY_TITLE = "title";

    @Override
    public String getPath() {
        return "/widget/nav_title";
    }

    @Override
    public boolean handle(WebView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        if (TextUtils.equals(uri.getPath(), getPath())) {
            String title = uri.getQueryParameter(KEY_TITLE);
            if (null != view && view.getContext() instanceof Activity) {
                ((Activity)view.getContext()).setTitle(Uri.decode(title));
            }
            return true;
        }
        return false;
    }
}
```

### 定制 RexxarContainerAPI

我们常常需要在 Rexxar Container 和 Rexxar Web 之间做数据交互。比如 Rexxar Container 可以为 Rexxar Web 提供一些计算结果。如果你需要提供一些由原生代码计算的数据给 Rexxar Web 使用，你就可以选择实现 RexxarContainerAPI 协议（Protocol），并实现以下三个方法：`getPath:`, `call:`。

在 Demo 中可以找到一个例子：`LocationAPI`。这个例子中，`LocationAPI` 返回了设备所在城市信息。当然，这个 ContainerAPI 仅仅是一个示例，它提供的是一个假数据，数据永远不会变化。你当然可以遵守 `RexxarContainerAPI` 协议，实现一个类似的但是数据是真实的功能。

```Java

    static class LocationAPI implements RexxarContainerAPI {

        @Override
        public String getPath() {
            return "/loc";
        }

        @Override
        public Response call(Request request) {
            Response.Builder responseBuilder = newResponseBuilder(request);
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("lat", "0.0");
                jsonObject.put("lng", "0.0");
                responseBuilder.body(ResponseBody.create(MediaType.parse(Constants.MIME_TYPE_JSON), jsonObject.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return responseBuilder.build();
        }
    }
```


### 定制 Rexxar Decorator

如果你需要修改运行在 Rexxar Container 中的 Rexxar Web 所发出的请求。例如，在 http 头中添加登录信息，你可以自定义OkHttpClient，`Rexxar.setOkHttpClient(OkHttpClient okHttpClient)`

在 Demo 中可以找到一个例子：`AuthInterceptor`。这个例子为 Rexxar Web 发出的请求添加了登录信息。

```Java

    public class AuthInterceptor implements Interceptor{

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            String url = request.url().toString();
            if (TextUtils.isEmpty(url)) {
                return null;
            }

            Request.Builder builder = request.newBuilder();
            builder.header("Authorization", "123456789");
            return chain.proceed(builder.build());
        }
    }

    // Rexxar初始化时设置
    Rexxar.setOkHttpClient(new OkHttpClient().newBuilder()
            .retryOnConnectionFailure(true)
            .addNetworkInterceptor(new AuthInterceptor())
            .build());
```

## 高级使用

### native调用js方法

```

    // 方法名
    RexxarWebView.callFunction(String functionName)
    
    // 方法名和json数据
    RexxarWebView.callFunction(String functionName, String jsonString)
    
```
  
## Partial RexxarWebView

如果，你发现一个页面无法全部使用 Rexxar 实现。你可以在一个原生页面内内嵌一个 `RexxarWebView`，部分功能使用原生实现，另一部分功能使用 Rexxar 实现。


Demo 中的 PartialRexxarViewController 给出了一个示例。



## License

Rexxar is released under the MIT license. See [LICENSE](LICENSE) for details.
