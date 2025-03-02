# uniapp经典蓝牙插件

uniapp通常提供的是BLE模式的蓝牙，但是uniapp的BLE模式不支持经典蓝牙，对于某些业务例如蓝牙耳机、蓝牙打印机等无法很好的支持。因此本人提供了经典蓝牙的uniapp扩展功能。

BluetoothModule 是java模块
dcloud_uniplugins 是插件接入示例，修改对应的uniapp插件功能
bluetoothAndroid 是typescript格式功能接口，大部分已封装为promise模式（侦听类回调除外）


## 接口功能
- enableBluetooth 启用蓝牙，返回蓝牙开启状态
- connectTo 连接蓝牙设备（手动断开情况下）
- searchConnect 指定蓝牙设备名进行搜索，设置X秒超时
- startBluetoothDiscovery 开始搜索蓝牙
- stopBluetoothDiscovery 停止搜索蓝牙
- getPairedDevices 获取已配对蓝牙设备
- connectBluetooth 连接蓝牙
- bluetoothStatusChange 蓝牙状态变化
- writeHexData 发送数据
- disconnectBluetooth 断开蓝牙
- unpairDevice 解除配对（并自动断开）

## 使用方法

```
import * as ClassicBle from '@/components/ClassicBluetooth/bluetoothAndroid'

// 按名称发现并连接蓝牙设备
await ClassicBle.connectDevice(`BTDevice-3DE5`)

// 指定设备名进行搜索并连接
await searchConnect(`BTDevice-3DE5`, 20)

// 断开蓝牙设备
await ClassicBle.disConnectDevice(`BTDevice-3DE5`)

// 获得状态
ClassicBle.getState((res) => {
console.log(`new state:${res.msg}`)
})

// 扫描蓝牙设备（设备名/地址/信号强度），获得的结果会包含经典蓝牙和BLE设备
ClassicBle.scanDevice((res) => {
console.log(res) // {name:'BTDevice-3DE5',address:'00:00:00:00:00:00',rssi: -100}
})

## TODO
持续优化代码

```
