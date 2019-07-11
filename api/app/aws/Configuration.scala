package io.flow.delta.aws

import software.amazon.awssdk.core.retry.RetryPolicy
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy
import software.amazon.awssdk.core.retry.conditions.RetryCondition
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration

@javax.inject.Singleton
class Configuration @javax.inject.Inject() () {
  private val retryPolicy = RetryPolicy.builder
    .retryCondition(RetryCondition.defaultRetryCondition)
    .backoffStrategy(BackoffStrategy.defaultStrategy)
    .numRetries(6)
    .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
    .build

  val aws = ClientOverrideConfiguration.builder.
    retryPolicy(retryPolicy).
    build
}
