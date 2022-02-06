package org.sebi.operators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.jgit.api.Git;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

@ControllerConfiguration
public class GitOpsRepoReconciler implements Reconciler<GitOpsRepo> {

    @Inject
    KubernetesClient client;

    @Inject
    org.quartz.Scheduler quartz;

    @Override
    public UpdateControl<GitOpsRepo> reconcile(GitOpsRepo gitOpsRepo, Context context) {
        if (gitOpsRepo.getSpec() != null) {
            
            if(gitOpsRepo.getSpec().getNamespace() == null) {
                gitOpsRepo.getSpec().setNamespace("default");
            }
            //if the job exist, we just update the jobdetails
            //TODO : remove Quartz once the new JOSDK is released when reconciliation time can be set
            String jobKey = gitOpsRepo.getMetadata().getName();
            try {
            if(quartz.checkExists(JobKey.jobKey(jobKey))){
                quartz.unscheduleJob(TriggerKey.triggerKey(gitOpsRepo.getMetadata().getName() + "_trigger"));
                quartz.deleteJob(JobKey.jobKey(jobKey));
                createOrUpdateJob(gitOpsRepo, jobKey);
            }
            else {
                createOrUpdateJob(gitOpsRepo, jobKey);
            }
            
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
        return UpdateControl.noUpdate();
    }

    private void createOrUpdateJob(GitOpsRepo gitOpsRepo, String jobKey) throws SchedulerException {
        JobDetail job;
        SimpleTrigger trigger;
        job = JobBuilder.newJob(MyJob.class)
        .withIdentity(jobKey)
        .usingJobData("url", gitOpsRepo.getSpec().getRepo())
        .usingJobData("ref", gitOpsRepo.getSpec().getRef())
        .usingJobData("namespace", gitOpsRepo.getSpec().getNamespace())
        .usingJobData("resourceDir",gitOpsRepo.getSpec().getResourceDir())
        .build();
        
        trigger = TriggerBuilder.newTrigger()
        .withIdentity(gitOpsRepo.getMetadata().getName() + "_trigger")
        .startNow()
        .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10)
                        .repeatForever())
        .build();
        quartz.scheduleJob(job, trigger);
    }

    private void createResource(File file, String namespace) {
        if (!file.isDirectory() && isYaml(file.getName())) {
            try {
                client.load(new FileInputStream(file))
                        .inNamespace(namespace)
                        .createOrReplace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isYaml(String fileName){    
        Optional<String> extension =  Optional.ofNullable(fileName)
              .filter(f -> f.contains("."))
              .map(f -> f.substring(fileName.lastIndexOf(".") + 1));
        if(extension.isPresent()){
            return extension.get().equals("yaml") || extension.get().equals("yml");
        }
        return false;
    }

    void performTask(String url, String ref,String resourceDir,String namespace) {
        File tmpDir;
        
        try {
            tmpDir = Files.createTempDirectory("tmpgit").toFile();
            Path dir = Paths.get(tmpDir.getAbsolutePath());
            
            Git git = Git.cloneRepository().setDirectory(tmpDir).setURI(url).call();
            if(ref!=null){
                git.checkout().setName(ref).call();
            }
            if(resourceDir != null){
                dir = Paths.get(tmpDir.getAbsolutePath() + "/" + resourceDir);
            }
            Files.walk(dir).forEach(path -> {
                createResource(path.toFile(),namespace);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MyJob implements Job {

        @Inject
        GitOpsRepoReconciler taskBean;

        public void execute(JobExecutionContext context) throws JobExecutionException {
            taskBean.performTask(context.getMergedJobDataMap().getString("url"),context.getMergedJobDataMap().getString("ref"),context.getMergedJobDataMap().getString("resourceDir"),context.getMergedJobDataMap().getString("namespace"));
        }

    }

    @Override
    public DeleteControl cleanup(GitOpsRepo gitOpsRepo, Context context) {
        String jobKey = gitOpsRepo.getMetadata().getName();
        try {
			quartz.unscheduleJob(TriggerKey.triggerKey(gitOpsRepo.getMetadata().getName() + "_trigger"));
            quartz.deleteJob(JobKey.jobKey(jobKey));
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
       
        return Reconciler.super.cleanup(gitOpsRepo, context);
    }
}
