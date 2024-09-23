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

package org.dromara.hutool.core.date.format;

import org.dromara.hutool.core.date.DateUtil;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FastDateFormatTest {
	private static final TimeZone timezone = TimeZone.getTimeZone("Etc/Utc");

	private static FastDateFormat getHutoolInstance(final String pattern) {
		return FastDateFormat.getInstance(pattern, timezone);
	}

	@Test
	public void yearTest() {
		final Date date = DateUtil.date(0L);

		assertEquals(
			"1970-01-01 00:00:00",
			getHutoolInstance("yyyy-MM-dd HH:mm:ss").format(date)
		);

		assertEquals(
			"1970-01-01 00:00:00",
			getHutoolInstance("YYYY-MM-dd HH:mm:ss").format(date)
		);

		assertEquals(
			"1970",
			getHutoolInstance("YYYY").format(date)
		);

		assertEquals(
			"70",
			getHutoolInstance("yy").format(date)
		);
	}

	@Test
	public void weekYearTest() {
		final Date date = DateUtil.date(0L);

		assertEquals(
			"70",
			new SimpleDateFormat("YY").format(date)
		);

		assertEquals(
			"70",
			getHutoolInstance("YY").format(date)
		);
	}
}