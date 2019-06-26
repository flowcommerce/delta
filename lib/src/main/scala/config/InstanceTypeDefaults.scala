package io.flow.delta.lib.config

import io.flow.delta.config.v0.models.InstanceType


case class MemoryDefault(
  instance: Int,  // actual memory of the instance type, per aws
  container: Int, // forced memory setting for the container
  jvm: Int        // jvm xmx setting for app running inside container
)

case class EbsDefault(
  ebs: Int
)

object InstanceTypeDefaults {

  // see for memory settings (in GB): https://aws.amazon.com/ec2/instance-types/
  def memory(typ: InstanceType): MemoryDefault = {
    // TODO: must be a way to get these programmatically
    typ match {
      case InstanceType.M4Large => MemoryDefault(8000, 7500, 6500)
      case InstanceType.M4Xlarge => MemoryDefault(16000, 15500, 13000)
      case InstanceType.M42xlarge => MemoryDefault(32000, 31000, 27000)

      case InstanceType.M5Large => MemoryDefault(8000, 7500, 6500)
      case InstanceType.M5Xlarge => MemoryDefault(16000, 15500, 13000)
      case InstanceType.M52xlarge => MemoryDefault(32000, 31000, 27000)

      case InstanceType.C4Large => MemoryDefault(3750, 3250, 2800)
      case InstanceType.C4Xlarge => MemoryDefault(7500, 7000, 6500)
      case InstanceType.C42xlarge => MemoryDefault(15000, 13500, 12000)

      case InstanceType.C5Large => MemoryDefault(3750, 3250, 2800)
      case InstanceType.C5Xlarge => MemoryDefault(7500, 7000, 6500)
      case InstanceType.C52xlarge => MemoryDefault(15000, 13500, 12000)

      case InstanceType.T2Micro => MemoryDefault(1000, 750, 675)
      case InstanceType.T2Small => MemoryDefault(2000, 1500, 1350)
      case InstanceType.T2Medium => MemoryDefault(4000, 3500, 3000)
      case InstanceType.T2Large => MemoryDefault(8000, 7500, 6500)

      case InstanceType.T3Micro => MemoryDefault(1000, 750, 675)
      case InstanceType.T3Small => MemoryDefault(2000, 1500, 1350)
      case InstanceType.T3Medium => MemoryDefault(4000, 3500, 3000)
      case InstanceType.T3Large => MemoryDefault(8000, 7500, 6500)
      case InstanceType.T3Xlarge => MemoryDefault(16000, 14000, 12000)

      // default to similar to t2.micro
      case InstanceType.UNDEFINED(_) => MemoryDefault(1000, 750, 675)
    }
  }

  def ebs(typ: InstanceType): EbsDefault =
    EbsDefault(
      // instance memory * 1.5 to allow heap dumps
      ebs = math.max(10000, math.ceil(memory(typ).instance * 1.5).toInt)
    )

}
