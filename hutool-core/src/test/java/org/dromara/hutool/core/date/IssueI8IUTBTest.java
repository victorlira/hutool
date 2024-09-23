/*
 * Copyright (c) 2024 Hutool Team and hutool.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.hutool.core.date;

import org.dromara.hutool.core.lang.Console;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class IssueI8IUTBTest {
	@Test
	void parseTest() {
		final Date parse = DateUtil.parse("May 8, 2009 5:57:51 PM");
		Console.log(parse);
	}
}