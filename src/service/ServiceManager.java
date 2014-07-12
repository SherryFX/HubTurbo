package service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Model;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.IGitHubConstants;
import org.eclipse.egit.github.core.service.CollaboratorService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.MilestoneService;

import service.updateservice.ModelUpdater;

public class ServiceManager {
	protected static final String METHOD_PUT = "PUT";
	protected static final String METHOD_POST = "POST";
	public static final String CHANGELOG_TAG = "[Change Log]\n";
	
	private static final ServiceManager serviceManagerInstance = new ServiceManager();
	private GitHubClientExtended githubClient;
	
	private CollaboratorService collabService;
	private IssueServiceExtended issueService;
	private LabelServiceFixed labelService;
	private MilestoneService milestoneService;
	
	private ModelUpdater modelUpdater;
	private Model model;
	private IRepositoryIdProvider repoId;
	
	public static final String STATE_ALL = "all";
	public static final String STATE_OPEN = "open";
	public static final String STATE_CLOSED = "closed";
	private int bufferSize = 8192;
	
	private ServiceManager(){
		githubClient = new GitHubClientExtended();
		collabService = new CollaboratorService(githubClient);
		issueService = new IssueServiceExtended(githubClient);
		labelService = new LabelServiceFixed(githubClient);
		milestoneService = new MilestoneService(githubClient);
		model = new Model();
	}

	public IRepositoryIdProvider getRepoId(){
		return repoId;
	}
	
	public Model getModel(){
		return model;
	}
	
	public Date getLastModelUpdateTime(){
		if(modelUpdater != null){
			return modelUpdater.getLastUpdateTime();
		}
		return null;
	}
	
	public void setupAndStartModelUpdate() {
		if(modelUpdater != null){
			stopModelUpdate();
		}
		if(repoId != null){
			modelUpdater = new ModelUpdater(githubClient, model);
			modelUpdater.startModelUpdate();
		}
	}
	
	public void restartModelUpdate(){
		if(modelUpdater != null){
			modelUpdater.stopModelUpdate();
			modelUpdater.startModelUpdate();
		}
	}
	
	public void stopModelUpdate(){
		if(modelUpdater !=  null){
			modelUpdater.stopModelUpdate();
		}
	}
	
	public static ServiceManager getInstance(){
		return serviceManagerInstance;
	}
	
	public boolean login(String userId, String password){
		githubClient.setCredentials(userId, password);
		try {
			GitHubRequest request = new GitHubRequest();
			request.setUri("/");
			githubClient.get(request);
		} catch (IOException e) {
			// Login failed
			return false;
		}
		return true;
	}
	
	public void setupRepository(String owner, String name){
		repoId = RepositoryId.create(owner, name);
		//TODO:
		model.loadComponents(repoId);
		setupAndStartModelUpdate();
	}
	
	public int getRemainingRequests(){
		return githubClient.getRemainingRequests();
	}
	
	public int getRequestLimit(){
		return githubClient.getRequestLimit();
	}
	
	/**
	 * Label Services
	 * */
	
	public List<Label> getLabels() throws IOException{
		if(repoId != null){
			return labelService.getLabels(repoId);
		}
		return new ArrayList<Label>();
	}
	
	public Label createLabel(Label ghLabel) throws IOException{
		if(repoId != null){
			return labelService.createLabel(repoId, ghLabel);
		}
		return null; //TODO:
	}
	
	public void deleteLabel(String label) throws IOException{
		if(repoId != null){
			labelService.deleteLabel(repoId, label);
		}
	}
	
	public Label editLabel(Label label , String name) throws IOException{
		if(repoId != null){
			return (Label)labelService.editLabel(repoId, label, name);
		}
		return null;
	}
	
	/**
	 * Milestone Services
	 * */
	public List<Milestone> getMilestones() throws IOException{
		if(repoId != null){
			return milestoneService.getMilestones(repoId, STATE_ALL);
		}
		return new ArrayList<Milestone>();
	}
	
	public Milestone createMilestone(Milestone milestone) throws IOException{
		if(repoId != null){
			return milestoneService.createMilestone(repoId, milestone);
		}
		return null;
	}
	
	public void deleteMilestone(int milestoneNum) throws IOException{
		if(repoId != null){
			milestoneService.deleteMilestone(repoId, milestoneNum);
		}
	}
	
	public Milestone editMilestone(Milestone milestone) throws IOException{
		if(repoId != null){
			return (Milestone)milestoneService.editMilestone(repoId, milestone);
		}
		return null;
	}
	
	
	/**
	 * Collaborator Services 
	 * */
	
	public List<User> getCollaborators() throws IOException{
		if(repoId != null){
			return collabService.getCollaborators(repoId);
		}
		return new ArrayList<User>();
	}
	
	/**
	 * Issue Services
	 * */
	
	public List<Issue> getAllIssues() throws IOException{
		if(repoId != null){
			Map<String, String> filters = new HashMap<String, String>();
			filters.put(IssueService.FIELD_FILTER, STATE_ALL);
			filters.put(IssueService.FILTER_STATE, STATE_ALL);
			return issueService.getIssues(repoId, filters);
		}
		return new ArrayList<Issue>();
	}
	
	public Issue createIssue(Issue issue) throws IOException{
		if(repoId != null){
			return issueService.createIssue(repoId, issue);
		}
		return null;
	}
	
	public Issue getIssue(int issueId) throws IOException{
		if(repoId !=  null){
			return issueService.getIssue(repoId, issueId);
		}
		return null;
	}
	
	public HashMap<String, Object> getIssueData(int issueId) throws IOException{
		if(repoId != null){
			return issueService.getIssueData(repoId, issueId);
		}
		return new HashMap<String, Object>();
	}
	
	public String getDateFromIssueData(HashMap<String, Object> issueData){
		return (String)issueData.get(IssueServiceExtended.ISSUE_DATE);
	}
	
	public Issue getIssueFromIssueData(HashMap<String, Object> issueData){
		return (Issue)issueData.get(IssueServiceExtended.ISSUE_CONTENTS);
	}
	
	public Issue editIssue(Issue latest, String dateModified) throws IOException{
		if(repoId != null){
			return (Issue)issueService.editIssue(repoId, latest, dateModified);
		}
		return null;
	}
	
	public Issue editIssueTitle(int issueId, String title) throws IOException{
		if(repoId != null){
			return issueService.editIssueTitle(repoId, issueId, title);
		}
		return null;
	}
	
	public Issue editIssueBody(int issueId, String body) throws IOException{
		if(repoId != null){
			return issueService.editIssueBody(repoId, issueId, body);
		}
		return null;
	}
	
	public void closeIssue(int issueId) throws IOException{
		if(repoId != null){
			issueService.editIssueState(repoId, issueId, false);
		}
	}
	
	public void openIssue(int issueId) throws IOException{
		if(repoId != null){
			issueService.editIssueState(repoId, issueId, true);
		}
	}
	
	/**
	 * Methods to work with comments data from github
	 * */
	
	public void logIssueChanges(int issueId, String changes){
		String changeLog = CHANGELOG_TAG + changes;
		try {
			createComment(issueId, changeLog);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Comment createComment(int issueId, String comment) throws IOException{
		if(repoId != null){
			return (Comment)issueService.createComment(repoId, Integer.toString(issueId), comment);
		}
		return null;
	}
	
	public List<Comment> getComments(int issueId) throws IOException{
		if(repoId != null){
			return issueService.getComments(repoId, issueId);
		}
		return new ArrayList<Comment>();
	}
	
	public void deleteComment(int commentId) throws IOException{
		if(repoId != null){
			issueService.deleteComment(repoId, commentId);
		}
	}
	
	public Comment editComment(Comment comment) throws IOException{
		if(repoId != null){
			return issueService.editComment(repoId, comment);
		}
		return null;
	}
	
	/**
	 * Methods to work with issue labels
	 * */
	
	public List<Label> setLabelsForIssue(int issueId, List<Label> labels) throws IOException{
		return labelService.setLabels(repoId, Integer.toString(issueId), labels);
	}
	
	/**
	 * Adds list of labels to a github issue. Returns all the labels for the issue.
	 * */
	public List<Label> addLabelsToIssue(int issueId, List<Label> labels) throws IOException{
		return labelService.addLabelsToIssue(repoId, Integer.toString(issueId), labels);
	}
	
	public void deleteLabelsFromIssue(int issueId, List<Label> labels) throws IOException{
		for(Label label : labels){
			deleteLabelFromIssue(issueId, label);
		}
	}
	
	public void deleteLabelFromIssue(int issueId, Label label) throws IOException{
		labelService.deleteLabelFromIssue(repoId, Integer.toString(issueId), label);
	}
	
	public void setIssueMilestone(int issueId, Milestone milestone) throws IOException{
		if(repoId != null){
			issueService.setIssueMilestone(repoId, issueId, milestone);
		}
	}
	
	public void setIssueAssignee(int issueId, User user) throws IOException{
		if(repoId != null){
			issueService.setIssueAssignee(repoId, issueId, user);
		}
	}

}