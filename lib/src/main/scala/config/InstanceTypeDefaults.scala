package io.flow.delta.lib.config

import io.flow.delta.config.v0.models.InstanceType
import play.api.Logger

object InstanceTypeDefaults {

  // see for memory settings (in GB): https://aws.amazon.com/ec2/instance-types/
  def memory(typ: InstanceType): Int = {
    // max memory for each instance type in MB
    // TODO: must be a way to get these programmatically
    typ match {
      case InstanceType.M4Large => 8000
      case InstanceType.M4Xlarge => 16000
      case InstanceType.M42xlarge => 32000

      case InstanceType.C4Large => 3750
      case InstanceType.C4Xlarge => 7500
      case InstanceType.C42xlarge => 15000

      case InstanceType.T2Micro => 1000
      case InstanceType.T2Small => 2000
      case InstanceType.T2Medium => 4000
      case InstanceType.T2Large => 8000

      case InstanceType.UNDEFINED(other) => {
        Logger.warn(s"Undefined instance type[$other]. Using default memory setting")
        700
      }
    }
  }

}
