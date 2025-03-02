/**
 * 经典蓝牙模块
 * @Author Jim 2025/02/24
 *
 */
// #ifdef APP-PLUS
export type MethodCallback = {
  success: boolean
  msg: string
}
const ifAndroid = uni.getSystemInfoSync().platform === 'android'
let bluetoothModule

if (ifAndroid) bluetoothModule = uni.requireNativePlugin('mb-bluetooth')

// const deviceList = []

const getPairedDevices = () => {
  return new Promise<any[]>((resolve, reject) => {
    bluetoothModule.getPairedDevices((res) => {
      resolve(res.devices)
    })
  })
}

const enableBluetooth = () => {
  return new Promise<boolean>((resolve, reject) => {
    bluetoothModule.enableBluetooth((data) => {
      if (data.success) {
        resolve(true)
      } else {
        reject(new Error('enableBluetooth error'))
      }
    })
  })
}

// const stopBluetoothDiscovery = () => {
//   return new Promise<boolean>((resolve, reject) => {
//     bluetoothModule.stopBluetoothDiscovery((res) => {
//       if (res.success) {
//         resolve(true)
//       } else {
//         reject(new Error('stopBluetoothDiscovery error'))
//       }
//     })
//   })
// }

const connectTo = (address: string) => {
  return new Promise<boolean>((resolve, reject) => {
    bluetoothModule.connectBluetooth(
      address,
      (res) => {
        if (res.success) {
          console.log(`经典蓝牙连接成功:${address}`)
          resolve(true)
        } else {
          reject(new Error('connectTo error'))
        }
      },
      (res) => {
        console.log('接收的数据', res.msg)
      }
    )
  })
}

const searchConnect = (deviceName: string, maxWaitSeconds: number) => {
  return new Promise<string>((resolve, reject) => {
    const stopTimer = setTimeout(() => {
      console.log(`exceed max wait ${maxWaitSeconds} seconds`)
      bluetoothModule.stopBluetoothDiscovery((res) => {
        if (res.success) {
          resolve(undefined)
        } else {
          reject(new Error('stopConnect error'))
        }
      })
      resolve(undefined) // 没有扫描到设备，就是undefined
    }, maxWaitSeconds * 1000)
    bluetoothModule.startBluetoothDiscovery((res) => {
      if (deviceName === res.name) {
        const address = res.address
        bluetoothModule.stopBluetoothDiscovery((res) => {
          if (res.success) {
            connectTo(address)
              .then(() => {
                clearTimeout(stopTimer)
                resolve(res.address) // 返回连接成功的地址
              })
              .catch((err) => {
                clearTimeout(stopTimer)
                reject(err)
              })
          } else {
            reject(new Error('stopConnect error'))
          }
        })
      }
    })
  })
}

const unpairDevice = (address: string) => {
  return new Promise<boolean>((resolve, reject) => {
    bluetoothModule.unpairDevice(address, (res: MethodCallback) => {
      if (res.success) {
        console.log('取消配对成功')
        resolve(true)
      } else {
        reject(new Error(res.msg))
      }
    })
  })
}

export const connectDevice = async (deviceName: string) => {
  try {
    if (ifAndroid) {
      // await enableBluetooth() // BLE已经enable后这里就不要再enable
      const pairedList: any[] = await getPairedDevices()
      const record = pairedList.find((item) => item.name === deviceName)
      if (record) {
        // 配对设备有记录，直接连
        console.log('有配置，直连经典蓝牙')
        await connectTo(record.address)
      } else {
        console.log('扫描了连接经典蓝牙')
        // 扫描了连(20秒)
        await searchConnect(deviceName, 20)
      }
    }
  } catch (e) {
    console.log('classic bluetooth error', e)
  }
}

// export const disconnectDevice = () => {
//   return new Promise<boolean>((resolve, reject) => {
//     bluetoothModule.disconnectBluetooth((res) => {
//       if (res.success) {
//         console.log('断开经典蓝牙成功')
//         resolve(true)
//       } else {
//         reject(new Error('disconnectDevice error'))
//       }
//     })
//   })
// }

export const disConnectDevice = async (deviceName: string) => {
  try {
    if (ifAndroid) {
      // await enableBluetooth() // BLE已经enable后这里就不要再enable
      const pairedList: any[] = await getPairedDevices()
      const record = pairedList.find((item) => item.name === deviceName)
      if (record) {
        // 有记录设备
        await unpairDevice(record.address)
      }
    }
  } catch (e) {
    console.log('classic bluetooth error')
  }
}


export const getState = bluetoothModule.bluetoothStatusChange
export const startScan = bluetoothModule.startBluetoothDiscovery
export const stopScan = () => {
  return new Promise<boolean>((resolve, reject) => {
    bluetoothModule.stopBluetoothDiscovery((res) => {
      if (res.success) {
        resolve(true)
      } else {
        resolve(false)
      }
    })
  })
}

// #endif
