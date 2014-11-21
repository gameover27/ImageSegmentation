/*
 * Copyright (C) 2014 Magdalena Neumann
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

package com.tum.imagesegmentation;

public final class Constants {
	public static final int GALLERY_IMAGE_ID = 42;
	public static final int GALLERY_SCRIBBLE_ID = 21;
	public static final float DRAW_TOLERANCE = 3;
	public static final int MAXPATH = 2;
	public static final int NUMBER_SAMPLES = 2000;
	public static final int NUMBER_SPLIT_LINES = 200;
	public static final float ALPHA =3.0f;
	public static final float BETA = 1.0f;
	public static final float TAU = 0.16f;
	public static final float THETA = 0.5f;
	public static final float CONTOUR_THRESHOLD = 0.7f;
	public static final double CONTOUR_WIDTH_RATIO = 0.01;
	public static final int SCRIBBLE_RED_FG = 0;
	public static final int SCRIBBLE_RED_BG = 255;
	public static final int SCRIBBLE_GREEN_FG = 0;
	public static final int SCRIBBLE_GREEN_BG = 0;
	public static final int SCRIBBLE_BLUE_FG = 255;
	public static final int SCRIBBLE_BLUE_BG = 0;

	private Constants() {
		throw new AssertionError();
	}
}
