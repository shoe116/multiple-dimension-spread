/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.yahoo.dataplatform.mds.block;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

public class TestBlockReadOffset{

  @Test
  public void T_newInstance_1(){
    new BlockReadOffset( 0 , 1 );
  }

  @Test
  public void T_compareTo_1(){
    BlockReadOffset o1 = new BlockReadOffset( 100 , 20 );
    BlockReadOffset o2 = new BlockReadOffset( 100 , 20 );

    assertEquals( o1.compareTo( o2 ) , 0 );
  }

  @Test
  public void T_compareTo_2(){
    BlockReadOffset o1 = new BlockReadOffset( 100 , 20 );
    BlockReadOffset o2 = new BlockReadOffset( 200 , 20 );

    assertEquals( o1.compareTo( o2 ) , -1 );
  }

  @Test
  public void T_compareTo_3(){
    BlockReadOffset o1 = new BlockReadOffset( 100 , 20 );
    BlockReadOffset o2 = new BlockReadOffset( 50 , 20 );

    assertEquals( o1.compareTo( o2 ) , 1 );
  }

}
