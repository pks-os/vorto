/**
 * Copyright (c) 2015-2016 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 */
package org.eclipse.vorto.repository.importer.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.vorto.repository.api.ModelInfo;
import org.eclipse.vorto.repository.core.FileContent;
import org.eclipse.vorto.repository.core.IUserContext;
import org.eclipse.vorto.repository.core.ModelResource;
import org.eclipse.vorto.repository.core.impl.parser.ModelParserFactory;
import org.eclipse.vorto.repository.core.impl.utils.BulkUploadHelper;
import org.eclipse.vorto.repository.core.impl.utils.ModelValidationHelper;
import org.eclipse.vorto.repository.core.impl.validation.ValidationException;
import org.eclipse.vorto.repository.importer.AbstractModelImporter;
import org.eclipse.vorto.repository.importer.FileUpload;
import org.eclipse.vorto.repository.importer.ValidationReport;
import org.eclipse.vorto.repository.web.core.exceptions.BulkUploadException;
import org.springframework.stereotype.Component;

/**
 * Imports (a bulk of) Vorto DSL Files to the Vorto Repository
 *
 */
@Component
public class VortoModelImporter extends AbstractModelImporter {

	public VortoModelImporter() {
		super(".infomodel", ".fbmodel", ".type", ".mapping", ".zip");
		
	}

	@Override
	public String getKey() {
		return "Vorto";
	}

	@Override
	public String getShortDescription() {
		return "Imports Vorto Informtion Models, defined with the Vorto DSL.";
	}
	
	protected boolean handleZipUploads() {
		return false;
	}

	@Override
	protected List<ValidationReport> validate(FileUpload fileUpload, IUserContext user) {
		if (fileUpload.getFileExtension().equalsIgnoreCase(EXTENSION_ZIP)) {
			BulkUploadHelper bulkUploadService = new BulkUploadHelper(getModelRepository(), getUploadStorage(),
					getUserRepository());
			return bulkUploadService.uploadMultiple(fileUpload.getContent(), fileUpload.getFileName(), user);
		} else {
			ModelValidationHelper validationHelper = new ModelValidationHelper(getModelRepository(),
					getUserRepository());
			try {
				final ModelInfo modelInfo = parseDSL(fileUpload.getFileName(),fileUpload.getContent());
				return Arrays.asList(validationHelper.validate(modelInfo, user));
			} catch (ValidationException ex) {
				return Arrays.asList(ValidationReport.invalid(null, ex.getMessage()));
			}
		}
	}

	@Override
	protected List<ModelResource> convert(FileUpload fileUpload, IUserContext user) {
		List<ModelResource> result = new ArrayList<ModelResource>();
		
		if (fileUpload.getFileExtension().equalsIgnoreCase(EXTENSION_ZIP)) {
			
			ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileUpload.getContent()));
			ZipEntry entry = null;
			
			try {
				while ((entry = zis.getNextEntry()) != null) {
					if (!entry.isDirectory() && ModelParserFactory.hasParserFor(entry.getName())) {
						result.add(parseDSL(entry.getName(), copyStream(zis,entry)));
					}
				}
			} catch (IOException e) {
				throw new BulkUploadException("IOException while getting next entry from zip file", e);
			}
		} else {
			final ModelResource modelInfo = (ModelResource) ModelParserFactory
					.getParser(fileUpload.getFileExtension()).parse(new ByteArrayInputStream(fileUpload.getContent()));
			result.add(modelInfo);
		}
		
		return Collections.unmodifiableList(result);
	}
	
	private static byte[] copyStream(ZipInputStream in, ZipEntry entry) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {			
			int size;
			byte[] buffer = new byte[2048];

			BufferedOutputStream bos = new BufferedOutputStream(out);
			
			while ((size = in.read(buffer, 0, buffer.length)) != -1) {
				bos.write(buffer, 0, size);
			}
			
			bos.flush();
			bos.close();
		} catch (IOException e) {
			throw new BulkUploadException("IOException while copying stream to ZipEntry", e); 
		}
		
		return out.toByteArray();
	}

	@Override
	protected void postProcessImportedModel(ModelInfo importedModel, FileContent originalFileContent, IUserContext user) {
		// no need to process the imported file further
	}
}
