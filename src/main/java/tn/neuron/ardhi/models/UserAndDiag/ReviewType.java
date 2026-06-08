package tn.neuron.ardhi.models.UserAndDiag;

public enum ReviewType {
    DIAGNOSIS, // Expert reviews the diagnosis itself (disease name)
    PROGRESS, // Expert reviews treatment progress via follow-up photo
    PREVENTION // Expert reviews prevention plan tasks (no photo)
}
