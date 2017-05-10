package org.ihtsdo.elasticsnomed.services;

import io.kaicode.elasticvc.api.BranchService;
import io.kaicode.elasticvc.domain.Branch;
import org.ihtsdo.elasticsnomed.TestConfig;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static io.kaicode.elasticvc.domain.Branch.BranchState.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class BranchServiceTest {

	@Autowired
	private BranchService branchService;

	@Test
	public void testCreateFindBranches() throws Exception {
		assertNull(branchService.findLatest("MAIN"));

		final Branch branch = branchService.create("MAIN");
		assertEquals(UP_TO_DATE, branch.getState());

		final Branch main = branchService.findLatest("MAIN");
		assertNotNull(main);
		assertNotNull(main.getInternalId());
		assertNotNull(main.getFatPath());
		assertNotNull(main.getBase());
		assertNotNull(main.getHead());
		assertEquals("MAIN", main.getFatPath());
		assertEquals(UP_TO_DATE, main.getState());

		assertNull(branchService.findLatest("MAIN/A"));
		branchService.create("MAIN/A");
		final Branch a = branchService.findLatest("MAIN/A");
		assertNotNull(a);
		assertEquals("MAIN/A", a.getFatPath());
		assertEquals(UP_TO_DATE, a.getState());

		assertNotNull(branchService.findLatest("MAIN"));
	}

	@Test
	public void testFindAll() {
		assertEquals(0, branchService.findAll().size());

		branchService.create("MAIN");
		assertEquals(1, branchService.findAll().size());

		branchService.create("MAIN/A");
		branchService.create("MAIN/A/AA");
		branchService.create("MAIN/C");
		branchService.create("MAIN/C/something");
		branchService.create("MAIN/C/something/thing");
		branchService.create("MAIN/B");

		final List<Branch> branches = branchService.findAll();
		assertEquals(7, branches.size());

		assertEquals("MAIN", branches.get(0).getFatPath());
		assertEquals("MAIN/A", branches.get(1).getFatPath());
		assertEquals("MAIN/C/something/thing", branches.get(6).getFatPath());
	}

	@Test
	public void testFindChildren() {
		assertEquals(0, branchService.findAll().size());

		branchService.create("MAIN");
		assertEquals(0, branchService.findChildren("MAIN").size());

		branchService.create("MAIN/A");
		branchService.create("MAIN/A/AA");
		branchService.create("MAIN/C");
		branchService.create("MAIN/C/something");
		branchService.create("MAIN/C/something/thing");
		branchService.create("MAIN/B");

		final List<Branch> mainChildren = branchService.findChildren("MAIN");
		assertEquals(6, mainChildren.size());

		assertEquals("MAIN/A", mainChildren.get(0).getFatPath());
		assertEquals("MAIN/C/something/thing", mainChildren.get(5).getFatPath());

		final List<Branch> cChildren = branchService.findChildren("MAIN/C");
		assertEquals(2, cChildren.size());
		assertEquals("MAIN/C/something", cChildren.get(0).getFatPath());
		assertEquals("MAIN/C/something/thing", cChildren.get(1).getFatPath());
	}

	@Test
	public void testBranchState() {
		branchService.create("MAIN");
		branchService.create("MAIN/A");
		branchService.create("MAIN/A/A1");
		branchService.create("MAIN/B");

		assertBranchState("MAIN", UP_TO_DATE);
		assertBranchState("MAIN/A", UP_TO_DATE);
		assertBranchState("MAIN/A/A1", UP_TO_DATE);
		assertBranchState("MAIN/B", UP_TO_DATE);

		makeEmptyCommit("MAIN/A");

		assertBranchState("MAIN", UP_TO_DATE);
		assertBranchState("MAIN/A", FORWARD);
		assertBranchState("MAIN/A/A1", BEHIND);
		assertBranchState("MAIN/B", UP_TO_DATE);

		makeEmptyCommit("MAIN");

		assertBranchState("MAIN", UP_TO_DATE);
		assertBranchState("MAIN/A", DIVERGED);
		assertBranchState("MAIN/A/A1", BEHIND);
		assertBranchState("MAIN/B", BEHIND);
	}

	private void assertBranchState(String path, Branch.BranchState status) {
		assertEquals(status, branchService.findLatest(path).getState());
	}

	private void makeEmptyCommit(String path) {
		branchService.completeCommit(branchService.openCommit(path));
	}

	@After
	public void tearDown() {
		branchService.deleteAll();
	}
}