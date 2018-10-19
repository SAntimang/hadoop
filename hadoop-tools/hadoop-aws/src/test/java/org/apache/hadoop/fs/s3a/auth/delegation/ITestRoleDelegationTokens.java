/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.s3a.auth.delegation;

import org.junit.Test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.s3a.S3AFileSystem;
import org.apache.hadoop.fs.s3a.auth.SessionCredentials;
import org.apache.hadoop.io.Text;

import static org.apache.hadoop.fs.s3a.auth.delegation.DelegationConstants.DELEGATION_TOKEN_ROLE_BINDING;
import static org.apache.hadoop.fs.s3a.auth.delegation.DelegationConstants.E_NO_SESSION_TOKENS_FOR_ROLE_BINDING;
import static org.apache.hadoop.fs.s3a.auth.delegation.DelegationConstants.ROLE_TOKEN_KIND;
import static org.apache.hadoop.fs.s3a.auth.delegation.RoleTokenBinding.E_NO_ARN;
import static org.apache.hadoop.test.LambdaTestUtils.intercept;

/**
 * Rerun the session token tests with a role binding.
 * Some tests will fail as role bindings prevent certain operations.
 */
public class ITestRoleDelegationTokens extends ITestSessionDelegationTokens {

  @Override
  protected String getDelegationBinding() {
    return DELEGATION_TOKEN_ROLE_BINDING;
  }

  @Override
  public Text getTokenKind() {
    return ROLE_TOKEN_KIND;
  }

  /**
   * Session credentials will not propagate with role tokens,
   * so the superclass's method will fail.
   * This subclass intercepts the exception which is expected.
   * @param fs base FS to bond to.
   * @param session session credentials from first DT.
   * @param conf config to use
   * @return null
   * @throws Exception failure
   */
  @Override
  protected AbstractS3ATokenIdentifier verifyAWSSessionCredentialPropagation(
      final S3AFileSystem fs,
      final SessionCredentials session,
      final Configuration conf) throws Exception {
    intercept(DelegationTokenIOException.class,
        E_NO_SESSION_TOKENS_FOR_ROLE_BINDING,
        () -> super.verifyAWSSessionCredentialPropagation(fs, session, conf));
    return null;
  }

  @Test
  public void testBindingWithoutARN() throws Throwable {
    describe("verify that a role binding only needs a role ARN when creating"
        + " a new token");

    Configuration conf = new Configuration(getConfiguration());
    conf.unset(DelegationConstants.DELEGATION_TOKEN_ROLE_ARN);
    try (S3ADelegationTokens delegationTokens2 = new S3ADelegationTokens()) {
      final S3AFileSystem fs = getFileSystem();
      delegationTokens2.bindToFileSystem(fs.getUri(), fs);
      delegationTokens2.init(conf);
      delegationTokens2.start();

      // cannot create a DT at this point
      intercept(IllegalStateException.class,
          E_NO_ARN,
          () -> delegationTokens2.createDelegationToken(
              new EncryptionSecrets()));
    }
  }
}
