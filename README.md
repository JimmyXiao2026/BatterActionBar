# BatterActionBar

Minecraft **1.21.11** / Fabric 客户端模组（id：`batteractionbar`，包名：`com.jimmyxiao.batteractionbar`）。

把服务器持续下发的动作栏和客户端其它 Mod 自己的动作栏分开显示，不再抢同一块位置；
同时提供公共接口，让其它 Mod 可以再往上加任意多条动作栏，各自独立定位。

## 它解决什么问题

原版里，服务端动作栏包和客户端 Mod 调用 `Gui#setOverlayMessage` 画出来的文字，
全都渲染在同一个位置（屏幕宽/2，屏幕高-68）。很多服务器每 tick 都下发动作栏，
结果就是中间那块位置一直被服务器占着，客户端 Mod（血条、状态提示等）没地方显示。

本模组按**来源**把这条动作栏归类，然后分别使用各自的坐标：

- 服务器动作栏：默认整体上移 30 像素，让出原版位置
- 客户端动作栏：默认仍在原版位置
- 其它 Mod 通过 API 注册的动作栏：默认原版位置，可在设置界面里拖到任意位置

## 判定来源的方式

只有两条包路径算「服务器」：

- `ClientboundSetActionBarTextPacket` → `ClientPacketListener#setActionBarText`
- `ClientboundSystemChatPacket(overlay = true)` → `ClientPacketListener#handleSystemChat`

其余途径（其它 Mod 直接调 `Gui#setOverlayMessage`、`LocalPlayer#displayClientMessage(…, true)`
等）一律算「客户端」。

实现上不是在包入口设一个布尔开关，而是登记那条 `Component` 对象的引用，
在 `Gui#setOverlayMessage` 收到同一个对象时才判定为服务端消息，
避免因为异常、延迟处理等情况把客户端消息误判成服务端的。

## 渲染方式

原版只有一个 `overlayMessageString` 槽位：谁后调用 `Gui#setOverlayMessage` 谁就把前一条顶掉，
所以服务端每 tick 下发动作栏时，客户端 Mod 自己的动作栏会被挤掉（反过来也一样），
只改位置是解决不了的。

因此本模组在 `Gui#setOverlayMessage` 入口把消息按来源分流到两条**独立通道**，
每条通道各自计时（60 tick、末 20 tick 淡出）、各自保存彩虹色标记，
再取消原版那条单槽位绘制，由模组同时画出两条，各用各的坐标。
所以服务器动作栏和客户端动作栏可以**同时显示**，互不顶掉。

绘制样式与原版一致（文字背景板、`nextStratum` 层级、淡出曲线）。
总开关关闭时直接放行原版渲染。

额外注册的动作栏由 `Gui#render` 的 TAIL 处补画，同样独立计时。

## 给其它 Mod 的公共接口

入口在 `com.jimmyxiao.batteractionbar.api`，客户端初始化里注册一次即可：

```java
import com.jimmyxiao.batteractionbar.api.ActionBarSlot;
import com.jimmyxiao.batteractionbar.api.BatterActionBarApi;
import net.minecraft.network.chat.Component;

// 注册：id 唯一、不可为 "server"/"client"；可选默认偏移（GUI 缩放像素）
ActionBarSlot statusBar = BatterActionBarApi.registerSlot(
    "mymod_status",                       // 配置里的键
    Component.literal("我的状态栏"),       // 设置界面里显示的名字
    0, 20                                 // 默认偏移 X, Y
);

// 显示
statusBar.show(Component.literal("正在采集"), 100);  // 100 tick 后消失，末 20 tick 淡出
statusBar.show(Component.literal("就绪"));           // 默认 60 tick
statusBar.showPersistent(Component.literal("挂机中")); // 一直显示，直到 clear()
statusBar.clear();

// 位置
int x = statusBar.offsetX();
statusBar.setOffset(0, 40);   // 代码里直接改，下次保存进配置
statusBar.resetOffset();      // 回到注册时的默认偏移

// 大小（缩放）
float s = statusBar.scale();  // 当前缩放
statusBar.setScale(1.5f);     // 0.25 ~ 4.0，超出会被钳制
statusBar.resetScale();       // 回到注册时给的默认缩放
```

## 大小（缩放）

每条动作栏都可以单独缩放，默认 1.0 倍（原版大小），可调范围 0.25 ~ 4.0 倍。
缩放围绕锚点 `(屏幕宽/2, 屏幕高-68)` 进行，即「位置不变、文字整体变大变小」，背景板跟着一起缩放。

- 设置界面：鼠标压在任意一条动作栏上**滚动滚轮**即可缩放，每次 0.1 倍
- 右键该条：位置和大小一起复位回默认
- 「重置全部位置与大小」：所有动作栏一起复位

注册时也可以直接指定默认大小：

```java
// id, 显示名, 默认 X, 默认 Y, 默认缩放
ActionBarSlot statusBar = BatterActionBarApi.registerSlot(
    "mymod_status", Component.literal("我的状态栏"), 0, 20, 1.2f);
```

- 注册后设置界面会自动多出一条该名字的动作栏，玩家可拖动、可缩放，位置与大小按 id 存进配置，不与内建两条冲突
- 重复注册同一个 id 会拿回原来那条（幂等），方便热重载场景
- 断开服务器连接时所有额外动作栏会自动清空
- 本模组的总开关关闭时，额外动作栏同样不绘制

## 设置界面

- 装了 **Mod Menu**：模组列表里点「设置」直接进
- 没装 Mod Menu：在「控制 → 按键绑定」里给「打开动作栏位置设置」绑一个键（默认未绑定）

界面里每条动作栏一行示例，**左键拖动**，**滚动滚轮缩放**，**右键复位这一条（位置 + 大小）**，松手自动保存。
另有「启用本模组」「隐藏服务器动作栏」「重置全部位置与大小」。
每条的标签会实时显示 X / Y / 当前倍率（如 `1.25x`）。
界面里那条横线标出原版动作栏的真实位置，方便对照。

## 配置

配置文件：`.minecraft/config/batteractionbar.json`

```json
{
  "enabled": true,
  "hideServer": false,
  "bars": {
    "server": { "x": 0, "y": -30, "scale": 1.0 },
    "client": { "x": 0, "y": 0, "scale": 1.0 },
    "mymod_status": { "x": 0, "y": 20, "scale": 1.2 }
  }
}
```

`x` / `y` 单位是 GUI 缩放像素，原点为原版动作栏位置 `(屏幕宽/2, 屏幕高-68)`；
`scale` 是缩放倍率，范围 0.25 ~ 4.0，缺省（或 ≤0）时按 1.0 处理，旧配置可以直接沿用。
`hideServer` 打开后服务器动作栏完全不画（消息本身照常过期，不影响其它逻辑）。

## 构建

需要 JDK 21（`build.bat` 里已经固定用 `C:\Program Files\Java\jdk-21.0.12.1`）：

```
.\build.bat          # 等价于 gradlew build --offline
.\run.bat            # 开发环境直接启动客户端
```

产物：`build/libs/batteractionbar-1.0.0.jar`。

依赖：Fabric Loader ≥ 0.19.5、Fabric API、Java 21；Mod Menu 可选（仅用于设置入口）。
其它 Mod 想调用本模组的 API 时，把上面的 jar 用 `compileOnly` 引一下即可。
