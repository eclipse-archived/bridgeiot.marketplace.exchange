/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Atos IT Solutions and Services GmbH, National University of Ireland Galway, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
lazy val generateSchema = taskKey[Unit]("Generate schema.json file")

fullRunTask(generateSchema, Compile, "tools.GenerateSchema")

packageBin in Compile <<= packageBin in Compile dependsOn generateSchema
