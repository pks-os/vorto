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
package org.eclipse.vorto.repository.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.vorto.repository.AbstractIntegrationTest;
import org.eclipse.vorto.repository.core.impl.InMemoryTemporaryStorage;
import org.eclipse.vorto.repository.core.impl.UserContext;
import org.eclipse.vorto.repository.core.impl.utils.BulkUploadHelper;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class ModelBulkImportTest extends AbstractIntegrationTest  {

	private BulkUploadHelper bulkUploadHelper;
	
	@Override
	public void beforeEach() throws Exception {
		super.beforeEach();
		bulkUploadHelper = new BulkUploadHelper(this.modelRepository, new InMemoryTemporaryStorage(), this.userRepository);
	}
	
	@Test
	public void testUploadValidModels() throws IOException {
		String fileName = "sample_models/valid-models.zip";
		List<ValidationReport> uploadResults = bulkUploadHelper.uploadMultiple(loadContentForFile(fileName),fileName, UserContext.user("admin"));
		assertEquals(3, uploadResults.size());
		verifyAllModelsAreValid(uploadResults);
	}
	
	@Test
	public void testUploadValidModelWithAlienFile() throws IOException {
		String fileName = "sample_models/valid-models-with-alien-file.zip";
		List<ValidationReport> uploadResults = bulkUploadHelper.uploadMultiple(loadContentForFile(fileName),fileName, UserContext.user("admin"));
		assertEquals(3, uploadResults.size());
		verifyAllModelsAreValid(uploadResults);
	}

	@Test
	public void testUploadOneMissingModels() throws IOException {
		String fileName = "sample_models/missing-models.zip";
		List<ValidationReport> uploadResults = bulkUploadHelper.uploadMultiple(loadContentForFile(fileName),fileName, UserContext.user("admin"));
		assertEquals(2, uploadResults.size());
		ValidationReport report = uploadResults.stream().filter(r -> r.getModel().getId().getName().equals("ColorLightIM")).findFirst().get();
		assertEquals(1,report.getUnresolvedReferences().size());
		verifyOneModelAreMissing(uploadResults);
	}
	
	@Test
	public void testUploadInvalidModels() throws IOException {
		String fileName = "sample_models/invalid-models.zip";
		List<ValidationReport> result = bulkUploadHelper.uploadMultiple(loadContentForFile(fileName),fileName, UserContext.user("admin"));
		assertEquals(2,result.size());
		assertFalse(result.get(0).isValid());
		assertFalse(result.get(1).isValid());
		assertNotNull(result.get(0).getModel());
		assertNotNull(result.get(1).getModel());
	}
	
	@Test
	public void testUploadDifferentModelTypesWithSameId() throws Exception {
		String fileName = "sample_models/modelsWithSameId.zip";
		List<ValidationReport> result = bulkUploadHelper.uploadMultiple(loadContentForFile(fileName),fileName, UserContext.user("admin"));
		assertEquals(2,result.size());
		assertFalse(result.get(1).isValid()); 	
	}
	
	@Test
	public void testUploadModelWithInvalidGrammar() throws Exception {
		String fileName = "sample_models/modelsWithWrongGrammar.zip";
		List<ValidationReport> result = bulkUploadHelper.uploadMultiple(loadContentForFile(fileName),fileName, UserContext.user("admin"));
		assertEquals(2,result.size());
		assertFalse(result.get(0).isValid());
		assertFalse(result.get(1).isValid()); 

	}
	
	private void verifyOneModelAreMissing(List<ValidationReport> uploadResults) {
		assertEquals(false, uploadResults.stream().allMatch(result -> result.isValid()));
		assertEquals((uploadResults.size() - 1), uploadResults.stream().filter(result -> result.getMessage() == null).count());
		assertEquals(1, uploadResults.stream().filter(result -> result.getMessage() !=null).count());
	}

	private void verifyAllModelsAreValid(List<ValidationReport> uploadResults) {
		assertEquals(true, uploadResults.stream().allMatch(result -> result.isValid()));
		assertTrue(uploadResults.stream().allMatch(result -> result.getMessage() == null));
	}
	
	private byte[] loadContentForFile(String fileName) throws IOException {
		return IOUtils.toByteArray(new ClassPathResource(fileName).getInputStream());
	}

}
