"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { BriefcaseIcon, CheckCircleIcon, ClockIcon, XCircleIcon } from "lucide-react"
import Link from "next/link"
import { api } from "@/lib/api"
import { useToast } from "@/hooks/use-toast"

export default function ApplicantDashboard() {
  interface Application {
    id: string;
    job: {
      id: string;
      title: string;
      company: { name: string };
    };
    status: "PENDING" | "REVIEWING" | "ACCEPTED" | "REJECTED";
    appliedAt: string;
  }

  const [applications, setApplications] = useState<Application[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [withdrawingId, setWithdrawingId] = useState<string | null>(null)
  const { toast } = useToast()

  useEffect(() => {
    const fetchApplications = async () => {
      try {
        // In a real implementation, this would fetch from the API
        const data = await api.getMyApplications();
        setApplications(data.data);
      } catch (error) {
        toast({
          title: "Error",
          description: "Failed to fetch applications. Please try again.",
          variant: "destructive"
        })
      } finally {
        setIsLoading(false)
      }
    }

    fetchApplications()
  }, [toast])

  const handleWithdrawApplication = async (applicationId: string, jobTitle: string) => {
    try {
      setWithdrawingId(applicationId)
      
      // Call the withdraw API
      await api.withdrawApplication(applicationId)
      
      // Remove the application from the local state
      setApplications(prev => prev.filter(app => app.id !== applicationId))
      
      toast({
        title: "Application Withdrawn",
        description: `Your application for ${jobTitle} has been withdrawn successfully.`,
        variant: "default"
      })
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to withdraw application. Please try again.",
        variant: "destructive"
      })
    } finally {
      setWithdrawingId(null)
    }
  }

  const getStatusIcon = (status: "PENDING" | "REVIEWING" | "ACCEPTED" | "REJECTED") => {
    switch (status) {
      case "PENDING":
        return <ClockIcon className="h-5 w-5 text-yellow-500" />
      case "REVIEWING":
        return <ClockIcon className="h-5 w-5 text-blue-500" />
      case "ACCEPTED":
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />
      case "REJECTED":
        return <XCircleIcon className="h-5 w-5 text-red-500" />
      default:
        return <ClockIcon className="h-5 w-5" />
    }
  }

  const getStatusText = (status: "PENDING" | "REVIEWING" | "ACCEPTED" | "REJECTED") => {
    switch (status) {
      case "PENDING":
        return "Pending"
      case "REVIEWING":
        return "Under Review"
      case "ACCEPTED":
        return "Accepted"
      case "REJECTED":
        return "Rejected"
      default:
        return status
    }
  }

  const canWithdraw = (status: "PENDING" | "REVIEWING" | "ACCEPTED" | "REJECTED") => {
    return status === "PENDING" || status === "REVIEWING"
  }
  
  


  return (
    <Tabs defaultValue="applications">
      <TabsList className="mb-4">
        <TabsTrigger value="applications">My Applications</TabsTrigger>
        {/* <TabsTrigger value="saved">Saved Jobs</TabsTrigger> */}
      </TabsList>

      <TabsContent value="applications">
        <Card>
          <CardHeader>
            <CardTitle>Job Applications</CardTitle>
            <CardDescription>Track the status of your job applications</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="text-center py-4">
                <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary mx-auto"></div>
                <p className="mt-2 text-sm text-muted-foreground">Loading your applications...</p>
              </div>
            ) : applications.length === 0 ? (
              <div className="text-center py-8">
                <BriefcaseIcon className="h-12 w-12 mx-auto text-muted-foreground" />
                <h3 className="mt-4 text-lg font-medium">No applications yet</h3>
                <p className="mt-2 text-sm text-muted-foreground">
                  You haven't applied to any jobs yet. Start exploring opportunities!
                </p>
                <Link href="/jobs">
                  <Button className="mt-4">Browse Jobs</Button>
                </Link>
              </div>
            ) : (
              <div className="space-y-4">
                {applications.map((application) => (
                  <div
                    key={application?.id}
                    className="flex flex-col md:flex-row justify-between items-start md:items-center p-4 border rounded-lg"
                  >
                    <div className="space-y-1">
                      <h3 className="font-medium">{application?.job?.title}</h3>
                      <p className="text-sm text-muted-foreground">{application?.job?.company.name}</p>
                      <p className="text-xs text-muted-foreground">
                        Applied on {new Date(application?.appliedAt).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="flex items-center mt-2 md:mt-0 gap-2">
                      <div className="flex items-center gap-1">
                        {getStatusIcon(application?.status)}
                        <span className="text-sm font-medium">{getStatusText(application?.status)}</span>
                      </div>
                      <div className="flex gap-2">
                        <Link href={`/jobs/${application?.job?.id}`}>
                          <Button variant="outline" size="sm">
                            View Job
                          </Button>
                        </Link>
                        {canWithdraw(application?.status) && (
                          <Button 
                            variant="destructive" 
                            size="sm"
                            onClick={() => handleWithdrawApplication(application.id, application.job.title)}
                            disabled={withdrawingId === application.id}
                          >
                            {withdrawingId === application.id ? "Withdrawing..." : "Withdraw"}
                          </Button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="saved">
        <Card>
          <CardHeader>
            <CardTitle>Saved Jobs</CardTitle>
            <CardDescription>Jobs you've saved for later</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-center py-8">
              <BriefcaseIcon className="h-12 w-12 mx-auto text-muted-foreground" />
              <h3 className="mt-4 text-lg font-medium">No saved jobs</h3>
              <p className="mt-2 text-sm text-muted-foreground">
                You haven't saved any jobs yet. Save jobs to apply to them later.
              </p>
              <Link href="/jobs">
                <Button className="mt-4">Browse Jobs</Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  )
}