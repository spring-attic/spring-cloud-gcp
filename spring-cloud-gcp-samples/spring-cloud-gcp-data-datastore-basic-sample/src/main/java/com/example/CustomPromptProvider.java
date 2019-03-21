/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * A prompt provider for the sample application web app.
 *
 * @author Dmitry Solomakha
 */
@Component
public class CustomPromptProvider implements PromptProvider {

	@Override
	public AttributedString getPrompt() {
		return new AttributedString("enter a command or type 'help' for info :> ",
				AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
	}

}
